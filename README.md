### Limited WIP
This is a plugin for IntelliJ IDEs to limit your work-in-progress (WIP).

It has two main features:
 - show notifications when current changelist size exceeds limit
 - automatically revert current changelist after a timeout


### Screenshots
<img src="https://github.com/dkandalov/limited-wip/blob/master/settings.png?raw=true" align="center"/>
<br/><br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/toolbar.png?raw=true" align="center"/>
<br/><br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/change-size-exceeded.png?raw=true" align="center"/>
<br/><br/>


### Why?
 - make smaller cohesive commits
 - stay focused with time or change size constraint


### How to use it?
There are probably many ways to use limited WIP, here are some ideas.

Small commits:
 - enable notification on change list size exceeding limit
 - make changes
 - if you get change size notification, commit or split you changes into several commits

"Hardcore" TDD:
 - choose a code kata (e.g. from [here](http://codingdojo.org/cgi-bin/index.pl?KataCatalogue))
 - set up auto-revert to 2 minutes or so
 - focus only on one part of [red-green-refactor](http://blog.cleancoder.com/uncle-bob/2014/12/17/TheCyclesOfTDD.html) cycle
 - commit after finishing part of the cycle
 - if changes are auto-reverted, consider if there is a smaller change you could make

Incremental refactoring:
 - choose a piece of code to refactor
 - set up auto-revert to 5-10 minutes
 - make refactoring changes before timeout
 - if changes are auto-reverted, find a way to make the same changes in incremental way

Learn your limits:
 - come up with a change you want to make
 - set up auto-revert to the time you think it'll take you to make the change
 - try making the change till you finish it before auto-revert
