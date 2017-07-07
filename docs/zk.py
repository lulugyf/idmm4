#coding=utf-8

from kazoo.client import KazooClient, KazooState
import kazoo
import logging
import time
import Queue
import os
import socket


class ZKCli:
    def __init__(self, hosts):
        logging.basicConfig()
        self.hosts = hosts
        zk = KazooClient(hosts=hosts)
        #zk.add_listener(self.my_listener)
        self.zk = zk
        #self.q = Queue.Queue()
        
    def start(self):
        # start command will be waiting until its default timeout
        self.zk.start()

    def stop(self):
        self.zk.stop()
        
    def __ck_path__(self, path):
        pp = path.split('/')
        for i in range(1, len(pp)):
            p = '/'.join(pp[0:i+1])
            if not self.zk.exists(p):
                self.zk.create(p, value=b'')

    def _wfunc(self, e):
        #print 'wfunc...', type(e), e.state, e.path, e.type, e.count(), e.index()
        if e.type == 'CHILD' and e.state == 'CONNECTED':
            path = e.path
            l = self.zk.get_children(path, self._wfunc)
            ol = self._old_ls
            self._old_ls = l
            self._out_func(ol, l)
    # watch_func(old_list, new_list)
    def list(self, path, watch=None):
        if watch is not None:
            self._out_func = watch
            l = self.zk.get_children(path, watch=self._wfunc)
            self._old_ls = l
            return l
        return self.zk.get_children(path)
    
    def get(self, path):
        return self.zk.get(path)
    
    # 创建临时节点， 成功返回True， 已经存在返回 False， 其余则失败
    def create(self, path, value, is_tmp=True):
        zk = self.zk
        try:
            r = self.zk.create(path, value=value, ephemeral=is_tmp)
            print '==1', r
            return True
        except kazoo.exceptions.NoNodeError,e:
            #check parent path
            ppath = path[0:path.rfind('/')]
            self.__ck_path__(ppath)
            print 'check parent path:', ppath
            try:
                self.zk.create(path, value=value, ephemeral=is_tmp)
                return True
            except kazoo.exceptions.NodeExistsError,e:
                print 'exists again...'
                return False
            except:
                print 'unknow error again'
                return False
        except kazoo.exceptions.NodeExistsError,e:
            print 'exists'
            return False
        except:
            print 'unknow error'
            return False
        
    def my_listener(self, state):
        if state == KazooState.LOST:
            print '==LOST'
            # Register somewhere that the session was lost
        elif state == KazooState.SUSPENDED:
            print '==SUSPENDED'
            # Handle being disconnected from Zookeeper
        elif state == KazooState.CONNECTED:
            print "==CONNECTED"
            #self.q.put('h')
        else:
            print '==state', state
            # Handle being connected/reconnected to Zookeeper

    def wait(self):
        #h = self.q.get()
        pass
        
    def close(self):
        self.zk.stop()
        self.zk.close()

    def delete(self, path):
        try:
            self.zk.delete(path)
        except kazoo.exceptions.NoNodeError,x:
            pass
        #print dir(self.zk)
        
def checkStartInfo(zkAddr, taskname, zkRoot='/db_sync_all'):
    zk = ZKCli(zkAddr)
    zk.start()
    #zk.wait()
    
    # get hostname and processid
    val = "%s--%d"%(socket.gethostname(), os.getpid())
    if zkRoot[0] != '/':
        zkRoot = '/' + zkRoot
    while True:
        if zk.create(zkRoot+'/'+taskname, val):
            break
        print 'already exists, sleep'
        time.sleep(5.0)
        
    
# def main():
#     zk = ZKCli('172.21.1.36:52181')
#     zk.start()
#
#     print "waitting..."
#     zk.wait()
#     print 'connected'
#     print 'created:', zk.create('/test1/temp1', b'123')
#
#
#     time.sleep(120.0)
#     zk.stop()
#
#
# if __name__ == '__main__':
#     #main()
#     checkStartInfo('127.0.0.1:2181', 'hello1')
#     while True:
#         print 'working loop', time.time()
#         time.sleep(10.0)

'''
节点上线和下线的通知消息：

pub_client: mon, src_topic: T_mon, dst_topic: T_mon, sub_client: mon

INSERT INTO `client_base_info_1` (`client_id`, `sub_system`, `client_desc`, `use_status`, `login_no`, `opr_time`, `note`) VALUES ('mon', 'event', 'publisher', '1', 'admin1', '2016-07-31 23:06:36', '');
INSERT INTO `dest_topic_info_1` (`dest_topic_id`, `dest_topic_desc`, `use_status`, `login_no`, `opr_time`, `note`) VALUES ('T_mon', 'event', '1', 'admin1', '2016-08-02 10:58:44', '');
INSERT INTO `src_topic_info_1` (`src_topic_id`, `src_topic_desc`, `use_status`, `login_no`, `opr_time`, `note`) VALUES ('T_mon', 'event', '1', 'admin1', '2016-07-31 23:07:32', '');
INSERT INTO `topic_mapping_rel_1` (`src_topic_id`, `attribute_key`, `attribute_value`, `dest_topic_id`, `use_status`, `login_no`, `opr_time`, `note`)
	VALUES ('T_mon', '_all', '_default', 'T_mon', '1', 'admin1', NULL, '');
INSERT INTO `ble_dest_topic_rel_1` (`dest_topic_id`, `BLE_id`, `use_status`, `login_no`, `opr_time`, `note`) VALUES ('T_mon', 10000001, '1', 'admin1', '2016-07-31 23:07:50', NULL);
INSERT INTO `topic_publish_rel_1` (`client_id`, `src_topic_id`, `use_status`, `login_no`, `opr_time`, `note`) VALUES ('mon', 'T_mon', '1', 'admin1', '2016-07-31 23:08:01', '');
INSERT INTO `topic_subscribe_rel_1` (`client_id`, `dest_topic_id`, `max_request`, `min_timeout`, `max_timeout`, `consume_speed_limit`, `max_messages`, `warn_messages`, `use_status`, `login_no`, `opr_time`, `note`)
	VALUES ('mon', 'T_mon', 20, 60, 600, 0, 0, 0, '1', NULL, '2016-08-07 00:20:59', 'event notify');

'''

def wfunc(ol, nl):
    add = list(set(nl) - set(ol))
    rmv = list(set(ol) - set(nl))
    print 'ADD', add
    print "RMV", rmv
    if len(nl) == 0:
        print "no broker available, can not send event message"
        return

    brokeraddr = str(nl[0])
    print 'broker addr:', repr(brokeraddr)
    from send import DMMClient
    client = DMMClient(brokeraddr)
    topic = 'T_mon'
    clientid = 'mon'
    if len(add) > 0:
        msgid = client.send(topic, clientid, 'Broker[s] get online:'+repr(add), 100, 13900)
        client.send_commit(topic, clientid, msgid)
    if len(rmv) > 0:
        msgid = client.send(topic, clientid, 'Broker[s] get offline:' + repr(rmv), 100, 13900)
        client.send_commit(topic, clientid, msgid)

def main_watch():
    zk = ZKCli('10.113.183.41:9781')
    zk.start()

    print "connecting..."
    zk.wait()
    print 'connected'

    # watching the change of path
    zk.list('/idmm4/broker', wfunc)


    print 'waiting...'
    while 1:
        time.sleep(120.0)

    zk.close()

def main_clear():
    zk = ZKCli('10.113.183.41:9781')
    zk.start()
    print "connecting..."
    zk.wait()
    print 'connected'

    zk.delete('/idmm4/supervisor')
    for n in zk.list('/idmm4/broker'):
        zk.delete('/idmm4/broker/'+n)
    for n in zk.list('/idmm4/ble'):
        zk.delete('/idmm4/ble/'+n)
    zk.close()
    

if __name__ == '__main__':
    main_clear()
