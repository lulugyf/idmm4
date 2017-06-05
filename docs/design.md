

### zk 里的数据结构:

``` ```
  
/(prefix)
 - ble
   - (ip):(port) `as ble_id`
   - (ip):(port)
 - blecmd
   - (ip):(port)-(ble_id)
   - (ip):(port)-(ble_id)
 - partitions_change  `这个用于触发broker更新分区数据`
 - partitions
   - (target_topic_id) ~ (client_id)
     - (part_id) (part_num):(part_status):(ble_id)
     - (part_id) (part_num):(part_status):(ble_id)
     - (part_id) (part_num):(part_status):(ble_id)
   - (target_topic_id) ~ (client_id)
     - ...
   - (target_topic_id) ~ (client_id)
     - ... 

各broker watch partitions_change 节点来获得分区调整的变更通知
   要求supervisor把需要同时生效的变化, 修改完成后在更新该标记, 可实现多个变更一次生效
   

### 命令的交互过程
    supervisor都以异步方式向ble发送命令, ble完成操作后异步回送应答, supervisor收到应答后, 
    根据任务情况再更新zookeeper分区数据
    如此的话, 有一个问题就是因为broker获得状态是延迟的, 就会有被拒绝的报错
      (要求修改ready -> leaving 操作是在最后进行， broker生产被拒绝的时候立刻同步更新该主题的分区数据， 然后重发请求, 更新过程会阻塞其他请求， 但应该值得）
    