# Scheduler

This branch contains differs in a method ```scheduleSyncTasks``` in ```Scheduler``` class that lets to schedule, if possible, different tasks of a given plan exactly at the same time instant. The sync relation between tasks is specified into the input CSV file by the column ```syncTasks```. For example, in the CSV file a possible value for ```syncTasks``` would be:

``` 2;3;4 ```

that indicates that the tasks 2,3 and 4 of a plan must start exactly at the same time instant.