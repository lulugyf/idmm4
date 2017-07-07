#coding=utf-8

from zk import ZKCli

def getParts(zk):
    qq = {}
    for q in zk.list('/idmm4/partitions'):
        parts = [] # pid pnum status bleid
        for pid in zk.list('/idmm4/partitions/'+q):
            data = zk.get('/idmm4/partitions/'+q+'/'+pid)
            pp = data.split('~')
            pp.insert(0, pid)
            parts.append(pp)
        qq[q] = parts
    return qq

def main():
    zk = ZKCli('10.113.183.41:9781')
    zk.start()
    print "connecting..."
    zk.wait()
    print 'connected'

    getParts(zk)

    zk.close()


if __name__ == '__main__':
    main()