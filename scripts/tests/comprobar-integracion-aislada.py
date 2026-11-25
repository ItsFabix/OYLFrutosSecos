"""Checks actual HTTP and file-backed H2 using only newly created child processes.
Build the three bootJar tasks first. Never connects to ports 8080/8081/8082.
Only test-owned child processes are stopped, including the simulated failure.
"""
from pathlib import Path
import os, json, subprocess, socket, time, uuid, urllib.request, urllib.error

root=Path(__file__).resolve().parents[2]; base=root/'build/isolated-service-checks'/('run-'+uuid.uuid4().hex[:12])
base.mkdir(parents=True,exist_ok=False)
java=Path(os.environ.get('JAVA_HOME','C:/Program Files/Android/Android Studio/jbr'))/'bin/java.exe'
token=uuid.uuid4().hex; admin=uuid.uuid4().hex
ports={}
for module in ['pickup-service','notification-service','server']:
    with socket.socket() as s:
        s.bind(('127.0.0.1',0)); ports[module]=s.getsockname()[1]
processes={}; logs=[]; checks=[]
def call(module,path,method='GET',body=None,headers=None):
    h={'Content-Type':'application/json'}; h.update(headers or {})
    req=urllib.request.Request(f'http://127.0.0.1:{ports[module]}'+path,method=method,headers=h,data=None if body is None else json.dumps(body).encode())
    with urllib.request.urlopen(req,timeout=6) as r:
        data=r.read(); return json.loads(data) if data else None
def start(module):
    env=os.environ.copy();env['OYL_SERVICE_TOKEN']=token;env['OYL_ADMIN_KEY']=admin
    env['OYL_NOTIFICATIONS_URL']=f'http://127.0.0.1:{ports["notification-service"]}'
    env['OYL_PICKUP_URL']=f'http://127.0.0.1:{ports["pickup-service"]}'
    output=open(base/(module+f'-{len(logs)}.log'),'wb');logs.append(output)
    jar=next((root/module/'build/libs').glob('*-SNAPSHOT.jar'))
    db=(base/module).as_posix()
    p=subprocess.Popen([str(java),'-jar',str(jar),f'--server.port={ports[module]}',f'--spring.datasource.url=jdbc:h2:file:{db};DB_CLOSE_ON_EXIT=FALSE','--spring.main.banner-mode=off'],cwd=base,env=env,stdout=output,stderr=subprocess.STDOUT)
    processes[module]=p
    health={'server':'/api/health','pickup-service':'/api/pickup-points','notification-service':'/api/notifications/health'}[module]
    deadline=time.monotonic()+45
    while time.monotonic()<deadline:
        if p.poll() is not None:raise RuntimeError(module+' exited; inspect isolated log')
        try: call(module,health); return
        except (OSError,urllib.error.URLError):time.sleep(.25)
    raise RuntimeError(module+' startup timeout')
def stop(module):
    p=processes.pop(module,None)
    if p is not None and p.poll() is None:p.terminate();p.wait(timeout=15)
def check(name,ok):
    assert ok,name;checks.append(name);print('PASS '+name,flush=True)
def events(order,count):
    deadline=time.monotonic()+12
    while time.monotonic()<deadline:
        try:
            result=call('server',f'/api/orders/{order}/notifications')
            if len(result)==count:return result
        except (OSError,urllib.error.URLError):pass
        time.sleep(.3)
    raise AssertionError(f'Events expected {count}')
def forbidden(module,path):
    try:call(module,path)
    except urllib.error.HTTPError as e:return e.code==403
    return False
try:
    for module in ports:start(module)
    products=call('server','/api/products');stock=next(p['stock'] for p in products if p['id']==1)
    check('catalog has eight seeded products',len(products)==8 and stock>2)
    points=call('pickup-service','/api/pickup-points')
    check('pickup delegated content equals direct content',len(points)==2 and points==call('server','/api/pickup-points'))
    check('admin orders require authorization',forbidden('server','/api/admin/orders'))
    check('internal notifications require authorization',forbidden('notification-service','/api/notifications/events?orderId='+str(uuid.uuid4())))
    body={'requestId':str(uuid.uuid4()),'customerAlias':'demostracion-aislada','items':[{'productId':1,'quantity':1}],'delivery':'PICKUP','contactPhone':'','contactAddress':''}
    first=call('server','/api/orders','POST',body);id1=first['id']
    check('new order pending',first['status']=='PENDING')
    repeated=call('server','/api/orders','POST',body)
    check('retry preserves order UUID',repeated['id']==id1)
    check('stock decremented exactly once',next(p['stock'] for p in call('server','/api/products') if p['id']==1)==stock-1)
    check('pending event generated',events(id1,1)[0]['status']=='PENDING')
    for status in ['CONFIRMED','PREPARED','DELIVERED']:
        response=call('server',f'/api/admin/orders/{id1}/advance','POST',headers={'X-OYL-ADMIN-KEY':admin})
        check('advance '+status,response['status']==status)
    original_events=events(id1,4)
    check('four unique original events in order',[e['status'] for e in original_events]==['PENDING','CONFIRMED','PREPARED','DELIVERED'] and len({e['eventId'] for e in original_events})==4)
    time.sleep(2)
    stop('notification-service')
    body['requestId']=str(uuid.uuid4());start_time=time.monotonic()
    second=call('server','/api/orders','POST',body);id2=second['id']
    check('purchase succeeds with notifications down',second['status']=='PENDING' and time.monotonic()-start_time<6)
    try:call('server',f'/api/orders/{id2}/notifications');unavailable=False
    except urllib.error.HTTPError as e:unavailable=e.code==503
    check('unavailable events reported honestly',unavailable)
    start('notification-service')
    check('outbox delivers pending event after recovery',events(id2,1)[0]['status']=='PENDING')
    check('previous four events persist after recipient restart',events(id1,4)==original_events)
    time.sleep(2)
    for module in list(processes):stop(module)
    for module in ports:start(module)
    check('delivered order persists after all services restart',call('server',f'/api/orders/{id1}')['status']=='DELIVERED')
    check('stock persists after sales restart',next(p['stock'] for p in call('server','/api/products') if p['id']==1)==stock-2)
    check('notifications persist without duplicate events',events(id1,4)==original_events and len(events(id2,1))==1)
    check('pickup H2 persists after restart',call('pickup-service','/api/pickup-points')==points)
    (base/'results.json').write_text(json.dumps({'passed':len(checks),'checks':checks,'orders':[id1,id2],'ports':ports,'isolated':True},indent=2),encoding='utf-8')
    print('ISOLATED CHECKS PASSED: '+str(len(checks)),flush=True)
    print('Report: '+str(base/'results.json'),flush=True)
finally:
    for module in list(processes):stop(module)
    for log in logs:log.close()
