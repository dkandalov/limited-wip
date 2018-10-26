[![Build Status](https://travis-ci.org/dkandalov/limited-wip.svg?branch=master)](https://travis-ci.org/dkandalov/limited-wip)

## Limited WIP
This is a plugin for IntelliJ IDEs to help you limit work-in-progress (WIP) by imposing constraints on your workflow.

It has three main components:
 - **Change size watchdog**: it shows notifications when current changelist size exceeds threshold
 - **Auto-revert**: it automatically reverts current changelist after a timeout (the timer resets on each commit)
 - **Test-commit-revert mode**: it reverts current changelist on failed test and opens commit dialog on passed test


## Why?
 - to make *really* small steps, focus on one thing at a time and commit as soon as it’s done
 - to learn from various constraints like reverting changes every 5 minutes as it's practiced at [code retreats](https://twitter.com/coderetreat)
 - to help you use particular constraints, not to impose them 
   (all components can be disabled and there are workarounds anyway, e.g. get reverted code from IDE local history)


## Change size watchdog
Whenever size of the current change list exceeds specified threshold, 
watchdog will show notification popup reminding to reduce amount of changes.
This is the least extreme constraint. It has been used on large scale enterprise projects.

You can find settings in `Preferences -> Other Settings -> Limited WIP`, where you can:
 - enable/disable component
 - change size threshold which is measured as the amount of lines changed ignoring empty lines
   (if file was deleted, then the amount of lines in the file). 
   There are no particular reasons for the predefined thresholds of 40, 80, 100, 120. These numbers are just a guess.
 - notification interval, i.e. how often watchdog will nag you after exceeding threshold
 - enable/disable statusbar widget showing current change size and change size threshold, 
   e.g. `Change size: 50/80` means that change size is 50 and threshold is 80.
   You can click on the widget to suppress notifications until next commit.
 - if commits are allowed after change size exceeded threshold.
   If enabled and change size is above threshold, you will not be able to open commit dialog 
   and will see notification popup instead (although you can still force commit by clicking on the link in the popup).


## Auto-revert
Every `N` minutes all changes in the current change list are automatically reverted.
You should make all necessary changes and commit before the timeout. Timer is reset on each commit.
Initially, auto-revert is stopped. To start/stop countdown until revert click on 
auto-revert widget in the statusbar or run `Start/stop auto-revert` action. 

This constraint has been used at [code retreats](https://twitter.com/coderetreat) with 5 minute timeout 
for quite a few years. It has been also useful on large scale enterprise projects with longer timeout 
(e.g. 30, 60, 120 minutes).

You can find settings in `Preferences -> Other Settings -> Limited WIP`, where you can:
 - enable/disable component (it is disabled by default)
 - timeout until revert in minutes
 - enable/disable notification on auto-revert (to make it clear why current changes disappeared)
 - enable/disable displaying timer in auto-revert widget. 
   Sometimes it can be useful to see how much time is left till revert. 
   In other cases, you might prefer not to see time left and just focus on making smallest change possible.


## TCR mode (test && commit || revert)
On a failing test revert current change list. On a passing test open commit dialog.
This is the most recent constraint so there isn't a lot of experience using it.
However, it's much more useful and enjoyable than it might seem initially.

You can find settings in `Preferences -> Other Settings -> Limited WIP`, where you can:
 - enable/disable component (it is disabled by default)
 - enable/disable notification on auto-revert (to make it clear why current changes disappeared)
 - enable/disable opening commit dialog on passed test 

I heard about the idea from [Kent Beck](https://twitter.com/KentBeck) mentioning Limbo and his
["test && commit || revert" blog post](https://medium.com/@kentbeck_7670/test-commit-revert-870bbd756864) in particular.
Originally, the idea comes from [Oddmund Strømme](https://twitter.com/jraregris), 
[Lars Barlindhaug](https://twitter.com/barlindh) and Ole Tjensvoll Johannessen.


## Screenshots
<img src="https://github.com/dkandalov/limited-wip/blob/master/screenshots/settings.png?raw=true" align="center"/>
<br/><br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/screenshots/change-limit-exceeded.png?raw=true" align="center"/>
<br/><br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/screenshots/commit-cancelled.png?raw=true" align="center"/>
<br/>


## History
The original version of the plugin called "Auto-revert" was conceived
at [LSCC meetup](http://www.meetup.com/london-software-craftsmanship/) in 2012
after having a chat with [Samir Talwar](https://twitter.com/SamirTalwar)
(it's easy to implement a basic version of auto-revert in bash but there are problems
like resetting timer after each commit and IntelliJ asking on revert from command line if you really want to load changes from file system).

Some time later (in 2015) after trying auto-revert on large legacy code bases I felt that it's a bit too harsh sometimes 
and I end up watching timer to stop it before auto-revert. 
To solve the problem watchdog component was created which doesn't revert but just notifies that there are too many changes.

At [FFS tech conf 2018](https://ffstechconf.org) [Limbo](https://medium.com/@kentbeck_7670/limbo-scaling-software-collaboration-afd4f00db4b) 
was mentioned and [test && commit || revert](https://medium.com/@kentbeck_7670/test-commit-revert-870bbd756864)
looked like a great fit for the plugin so I had to implement it.

I hope there will be more components in the future.
If you have an idea, feel free to create github issue or [tweet it to me](https://twitter.com/dmitrykandalov).