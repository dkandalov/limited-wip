## Limited WIP IntelliJ plugin

This is a plugin for IntelliJ IDEs to limit the amount of changes you make at a time. It has two main features:
 - notification when current change list size exceeds limit
 - reverting current change list after a timeout (ctrl+shift+A to start/stop)

Here are some screenshots of plugin preferences/toolbar/notifications:
<img src="https://github.com/dkandalov/limited-wip/blob/master/settings.png?raw=true" align="center"/>
<br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/toolbar.png?raw=true" align="center"/>
<br/><br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/change-size-exceeded.png?raw=true" align="center"/>
<br/><br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/commit-was-cancelled.png?raw=true" align="center"/>
<br/><br/>


## Why?

This plugin is intended as a tool to limit your work-in-progress (and prevent yourself from cheating).
It's based on the following assumptions:
 - small focused changes are better than changes which go on until "everything is done"
 - people are good at self-deception and not following rules


## How to use it?

There are probably many ways to make use of limited WIP, here are some ideas.

Smaller commits:
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
