[![Build Status](https://travis-ci.org/dkandalov/limited-wip.svg?branch=master)](https://travis-ci.org/dkandalov/limited-wip)

## Limited WIP
This is a plugin for IntelliJ IDEs to limit work-in-progress (WIP) by adding constraints to your normal workflow in IDE.

It has three main components (each one can be enabled/disabled):
 - **Change size watchdog**: shows notifications when current changelist size exceeds threshold
 - **Auto-revert**: automatically reverts current changelist after a timeout (the timer is reset after each commit)
 - **TCR mode (test && commit || revert)**: you can only commit after running a test;
 if the test fails, current changelist is reverted; if the test passes, changes are automatically committed


## Why?
 - focus on one thing at a time, make *really* small steps and commit as soon as you're done
 - practice various constraints as if you're at a [code retreat](https://twitter.com/coderetreat)
 - explore your limits and learn new refactoring, coding and problem solving techniques


## Change size watchdog
Whenever size of the current change list exceeds specified threshold, 
watchdog will show notification popup reminding to reduce amount of changes.
This is the least extreme constraint and has been used on large scale enterprise projects.

You can find settings in `Preferences -> Other Settings -> Limited WIP`, where you can:
 - enable/disable component
 - change size threshold which is measured as the amount of lines changed ignoring empty lines
   (if file was deleted, then the amount of lines in the file). 
   There are no particular reasons for the predefined thresholds of 40, 80, 100, 120. These numbers are just a guess.
 - notification interval, i.e. how often watchdog will nag you after exceeding threshold
 - enable/disable statusbar widget showing current change size and change size threshold, 
   e.g. `Change size: 50/80` means that change size is 50 and threshold is 80.
   You can click on the widget to suppress notifications until next commit.
 - enable/disable commits after change size exceeded threshold.
   If enabled and change size is above threshold, you will not be able to open commit dialog 
   and will see notification popup instead (although you can still force commit by clicking on the link in the popup).
 - choose excluded files which will not be monitored by the watchdog.
   Use `;` to separate patterns. Accepted wildcards: `?` - exactly one symbol;
   `*` - zero or more symbols; `/` - path separator; `/**/` - any number of directories. 


## Auto-revert
 - Timer starts as soon as there are any changes in version control.
 - Timer resets when there are no more changes (because of commit, revert or undo).
 - When timer reaches 0 seconds, all changes are automatically reverted.

This constraint has been used at [code retreats](https://twitter.com/coderetreat) with 5 minute timeout 
for quite a few years. It has been useful on large scale enterprise projects with longer timeouts 
(e.g. 30, 60, 120 minutes). And it also seems to work well in combination with TCR.

You can find settings in `Preferences -> Other Settings -> Limited WIP`, where you can:
 - enable/disable component (it is disabled by default)
 - timeout until revert in minutes or seconds
 - enable/disable notification on auto-revert (to make it clear why current changes disappeared)
 - enable/disable displaying timer in auto-revert widget. 
   Sometimes it can be useful to see how much time is left till revert. 
   In other cases, you might prefer not to see time left and just focus on making smallest change possible.

This is not part of the workflow, but you can pause the timer by clicking on auto-revert widget in IDE toolbar. 


## TCR mode (test && commit || revert)
 - You're not allowed to commit without running a test.
 - If the test fails, current change list is reverted. 
 - If the test passed, changes are committed.

This is the most recent constraint so there isn't a lot of experience using it.
However, it's much more useful and enjoyable than it might seem initially.

You can find settings in `Preferences -> Other Settings -> Limited WIP`, where you can:
 - enable/disable component (disabled by default)
 - choose action on passed test, it can be
    - commit
    - amend commit (will open commit dialog if the last commit has been pushed or different set of tests has been run compared to last execution)
    - commit and push (if "push" is supported by VCS)
    - open commit dialog
 - choose commit message source, it can be
    - last commit
    - current changelist name
 - enable/disable notification on revert (to make it clear why current changes disappeared)
 - enable/disable revert of test code (aka [relaxed TCR](https://medium.com/@tdeniffel/tcr-variants-test-commit-revert-bf6bd84b17d3))
 - exclude files from revert in case some of the tests are not marked as test source root in IDE

I heard about the idea from [Kent Beck](https://twitter.com/KentBeck) mentioning Limbo and his
["test && commit || revert" blog post](https://medium.com/@kentbeck_7670/test-commit-revert-870bbd756864) in particular.
Originally, the idea comes from [Oddmund Strømme](https://twitter.com/jraregris), 
[Lars Barlindhaug](https://twitter.com/barlindh) and Ole Tjensvoll Johannessen.


## Screenshots
<img width="50%" src="https://github.com/dkandalov/limited-wip/blob/master/screenshots/settings.png?raw=true" align="center" alt="settings screenshot"/>
<br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/screenshots/change-limit-exceeded.png?raw=true" align="center" alt="change limit exceeded notification"/>
<br/>
<img src="https://github.com/dkandalov/limited-wip/blob/master/screenshots/commit-cancelled.png?raw=true" align="center" alt="commit cancelled notification"/>
<br/>


## Videos
 - [Mobbing FizzBuzzWoof with TCR at SoCraTes UK 2019](https://www.youtube.com/watch?v=tmRRlzPWyYA) 
 - [Tennis kata with TCR at SoCraTes UK 2019](https://www.youtube.com/watch?v=H0z_NhQIOHQ) 
 - [FizzBuzzWoof with TCR at SoCraTes UK 2019](https://www.youtube.com/watch?v=3s14AtA5R48) 


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
