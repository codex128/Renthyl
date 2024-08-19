Renthyl is a modular FrameGraph-based rendering pipeline designed with efficiency in mind.

:warning: **Warning!** :warning:<br>
Renthyl is still under development and should not be used for production yet. It additionally uses libraries that have not been deployed.

## Features

* Allows for the use of modern rendering paradigms, such as deferred and forward++.
* Code-first design makes constructing and manipulating FrameGraphs incredibly easy.
* Easily control the layout of the FrameGraph using game logic.
* Designed to be totally savable, so FrameGraphs can be saved and loaded from files.
* Culling algorithm ensures unnecessary operations are skipped.
* Resource manager minimizes the creation and binding of resources by reallocating resources where possible.
* Allows for stress-free multithreading.
* Tree-like Structure allows individual groups of passes to be exported and shared.

## Get Started

1. Download Renthyl (including assets).

2. Download dependencies
   * [JMonkeyEngine](https://github.com/codex128/jmonkeyengine/tree/pipelineApi) (experimental branch).
   * [Boost](https://github.com/codex128/Boost) (master branch)
   * Java JDK8

3. Initialize Renthyl in your JMonkeyEngine application.
   
```java
Renthyl.initialize(application);
FrameGraph fg = Renthyl.forward(assetManager);
viewPort.setPipeline(fg);
```

4. Run the project.

## Learn Renthyl

A full guide on how to effectively use Renthyl can be found at this repository's [wiki](https://github.com/codex128/FrameGraph/wiki).
