The Brickening
=============

The full source code to an Arkanoid ripoff I wrote for Android back in 2010. Parts of it are deeply embarrassing, but that comes with the territory of releasing a big lump of code you wrote four years ago and haven't touched since. A few things to note:

* My first large Java project
* Directly used the Android canvas APIs for rendering
* Custom game loop class (GameView.java)
* A few neat composite animation effects (alpha fade and slide out, etc... If I knew about Core Animation back then, I would have written that differently)
* Random async tasks for communicating with web servers or dispatching events on the UI thread
* A horrifyingly long and complex updateGame() method, with crappy collision detection
* Also, the level sharing/high scores were literally my first web service ever (had no idea what I was doing)
* I didn't read up on design patterns until the year after I wrote this, so... yeah.

There are other components to it that I don't have anymore. The aforementioned web service part of it (which was in PHP with a MySQL database), as well as the level editor which was a C# desktop app (which I wrote and ran on Ubuntu Linux!). If I dig really hard I can probably find the level editor.
