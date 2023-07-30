# Tetrad

The purpose of Tetrad is to make algorithms available in Java to do causal inference. Here is the [Dietrich College Carnegie Mellon University web page for Tetrad](https://www.cmu.edu/dietrich/news/news-stories/2020/august/tetrad-sail.html).
Here is the [Simon Initiative page for Tetrad](https://www.cmu.edu/simon/open-simon/toolkit/tools/learning-tools/tetrad.html).

Here is our [project web page](https://sites.google.com/view/tetradcausal) here with current links for artifacts, a list of contributors, and a bit of history.

Here is the web page for the [Center for Causal Discovery](https://www.ccd.pitt.edu/), which also supports the latest version of Tetrad and Causal Command.

## Setting up Java

You will need to set up Java on your machine. For setting up Java, see our Wiki
article, [Setting up Java for Tetrad](https://github.com/cmu-phil/tetrad/wiki/Setting-up-Java-for-Tetrad). Note that as of version 7.5.0, we will be supporting separate builds for JDK 1.8 and JDK 17 (which will work for > 1.8). This is because there are many users who are unable to update from JDK 1.8, for whatever reason, and we would like to support these users. The codebase of Tetrad throughout uses language level 8, so this is feasible.

## Tetrad GUI Application

To download the current jar you can use to launch the Tetrad GUI, click this link:

https://s01.oss.sonatype.org/content/repositories/releases/io/github/cmu-phil/tetrad-gui/7.4.0/tetrad-gui-7.4.0-launch.jar.

You may be able to launch this jar by double clicking the jar file name, though on a Mac, this presents some [security challenges](https://github.com/cmu-phil/tetrad/wiki/Dealing-with-Tetrad-on-a-Mac:--Security-Issues). In any case, on all platforms, the jar may be launched at the command line (with a specification of the amount of RAM you will allow it to use) using this command:

```
java -Xmx[g]G -jar *-launch.jar
```

where [g] is the maximum number of Gigabytes you wish to allocate to the process.

## Command Line

We have a tool, [Causal Command](https://github.com/bd2kccd/causal-cmd), that lets you run Tetrad algorithms at the command line.

## Python and R Integration

For Python integration, please see our (still new) [py-tetrad Python project](https://github.com/cmu-phil/py-tetrad), which shows how to integrate arbitrary Java code in the Tetrad project into a Python workflow using the [JPype Python project](https://jpype.readthedocs.io/en/latest/).

Also, please see the [causal-learn Python package](https://causal-learn.readthedocs.io/en/latest/), translating some Tetrad algorithms into Python and adding some algorithms not in Tetrad, now part of the [py-why space](https://github.com/py-why)

We added [a method to use Tetrad algorithms in R via py-tetrad](https://github.com/cmu-phil/py-tetrad/blob/main/pytetrad/R/). This is work in progress.

## Documentation

If you're new to Tetrad, here is a [Tutorial](https://rawgit.com/cmu-phil/tetrad/development/tetrad-gui/src/main/resources/resources/javahelp/manual/tetrad_tutorial.html). Also, here is
our [Manual](https://htmlpreview.github.io/?https:///github.com/cmu-phil/tetrad/blob/development/docs/manual/index.html). If you like to watch thought-provoking lectures, here are some [lectures on the Center for Causal Discovery site](https://www.ccd.pitt.edu/video-tutorials/).

In addition, here are our [Javadocs](https://www.phil.cmu.edu/tetrad-javadocs/7.4.0).

## Install

All artifacts are published in the [Maven Central Repository](https://s01.oss.sonatype.org/content/repositories/releases/io/github/cmu-phil ). This includes all downloadable jars and Javadocs.

Here is our [GitHub URL](https://github.com/cmu-phil/tetrad). Also, here are some [instructions on how to set this project up in IntelliJ IDEA](https://github.com/cmu-phil/tetrad/wiki/Setting-up-Tetrad-in-IntelliJ-IDEA). You can run the Tetrad lifecycle package target and launch the "-launch" jar that is built in the target directory.

The project contains well-developed code in these packages:

* tetrad
* pitt
* tetradapp

The tetrad-lib package contains the model code; the tetrad-gui package contains the view (GUI) code.

## Bug Reports

Please submit issue requests for Tetrad using our [Issue Tracker](https://github.com/cmu-phil/tetrad/issues). We will try to the extent possible to resolve all reported issues before [releasing new versions of Tetrad](https://github.com/cmu-phil/tetrad/releases). This may involve moving items to our [Wish List](https://github.com/cmu-phil/tetrad/wiki/Current-Wish-List).

## Open Code

All of our code is public and we welcome suggestions, especially suggestions that improve clarity or performance of our code.

If you're writing code using (or for!) Tetrad in either [Java](https://github.com/cmu-phil/tetrad) or [Python](https://github.com/cmu-phil/py-tetrad) (or R, which we're working toward!), thank you! Please contribute if you are doing amazing work, or publish and send us links to your papers. Please keep us abreast of how Tetrad could be improved for your applications; we will do what we can.

## Citation

Please cite as:

```
@inproceedings{ramsey2018tetrad,
  title={TETRAD—A toolbox for causal discovery},
  author={Ramsey, Joseph D and Zhang, Kun and Glymour, Madelyn and Romero, Ruben Sanchez and Huang, Biwei and Ebert-Uphoff, Imme and Samarasinghe, Savini and Barnes, Elizabeth A and Glymour, Clark},
  booktitle={8th international workshop on climate informatics},
  year={2018}
}
```
