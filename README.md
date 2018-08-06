# How to run
1) Make sure Scala and Sbt are installed
2) Run "sbt run" from root project directory. The provided input can be execute using: "sbt run < src/main/resources/sessionize/input.txt"

# Assumptions
1) Data will fit into memory
2) Can't assume a particular timestamp range.

# Design
The approach will be to turn the input into a stream of cleaned up valid Input messages. Then the messages will
be grouped by device id and each group will be sorted by timestamp. Once sorted the items will be combined using a fold
operation to merge items of the same activity that are next to each other (or within the timeout). The fold will
produce Output messages that will denote each interval of watching a channel. Once all the inputs
are processed the Output messages will be send to standard out.

# Shortcomings
1) Data has to fit into memory
2) Single threaded
3) Single node

# Alternative 1
In a real world solution I would create a microservice that listens to the inputs, probably through http, that come
from the tvs and immediately place the messages into a kafka queue. The key would be the device id and the timestamp
would be overridden by the timestamp coming from the messages. The biggest problem would be how define the custom
windowing logic since we are not doing a timestamp based window. One possible solution to that would be to:

1) Whenever a new value arrives for a device, we put it into a list of all values for that device within the supported
timestamp range.
2) Whenever we detect a continuous section of items between activity switches, we output that as a single
item with from-to to the next topic.
3) We also have to take care of timeouts so that a device that stops sending us messages would produce it's results. As
well as the first message since we wouldn't know when the stream is to start.
In Kafka streams there is a concept of a KTable where whenever an item comes in for the same key, kafka provides a call
where these items should be turned into a single item. Alternatively some form of stateful map function can be used to
maintain the list defined in 1.

# Alternative 2
To simplify this problem perhaps it's possible to define a time based window of 1 hour, for example. We can then use
this window to delay outputting the messages for each hour until we get all the signals, or a timeout occurs. This would
allow much simpler logic on the combining of the messages. The side effect would be the delay as well as the limiting of
all outputs to be bound to their hour. i.e. if the device is performing the same activity when the hour changes, we
would output 2 event, 1 that would end at the last timestamp of the previous hour and the other that would start at the
beginning of the next hour. Within this alternative it would also be possible to merge the hours together in a post
processing step if necessary as at that point we'd have less data to worry about.
