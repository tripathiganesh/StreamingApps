#### Considerations when using dynamic allocation and the Capacity Scheduler for Spark jobs.

##### While using the Capacity Scheduler, the  ```DefaultResourceAllocator``` (default) value causes only memory to be accounted for and not the CPU. This can cause an oversubscription to nodes or cause all containers to run on one node. When dynamic allocation is enabled by default, Spark runs all the executors on a single node does not utilize the cluster efficiently.

Cloudera recommends using the Fair Scheduler.

`Option 1`
Use the Fair Scheduler

`Option 2`
Use the Capacity Scheduler and change the dynamic allocation setting.

Use the DominantResourceAllocator value which considers vcores and distributes containers on all nodes.
To set this value in the Capacity Scheduler, do the following:
From Cloudera Manager, navigate to YARN > Configuration > Resource Manager > Capacity Scheduler Configuration Advanced Configuration Snippet (Safety Valve)
Add the following value:
```xml
<property>
<name>yarn.resourcemanager.scheduler.class</name>
<value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler</value>
</property>
```
and
```xml
<property>
<name>yarn.scheduler.capacity.resource-calculator</name>
<value>org.apache.hadoop.yarn.util.resource.DominantResourceCalculator</value>
</property>
```
*Save changes*

*Restart YARN*

Setting Dynamic Allocation (Optional)
When dynamic allocation is enabled (the default setting), executors are removed when idle. Dynamic allocation is not effective in Spark Streaming as data comes in every batch, and executors run whenever data is available. If the executor idle timeout is less than the batch duration, executors are constantly being added and removed. However, if the executor idle timeout is greater than the batch duration, executors are never removed. Thus, Spark runs all the executors on a single node and does not utilize the cluster efficiently.

Cloudera recommends that you disable the dynamic allocation property spark.dynamicAllocation.enabled when running streaming applications. When dynamic allocation is disabled, Spark runs all the executors throughout the cluster.
From Cloudera Manager, navigate to 

*Spark  > Configuration > Gateway Default Group > Enable Dynamic Allocation*

*spark.dynamicAllocation.enabled*

*Uncheck the default value check box.*

*Save Changes*
*Restart Spark*
