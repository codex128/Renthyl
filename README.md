
# Renthyl

[![](https://jitpack.io/v/codex128/Renthyl.svg)](https://jitpack.io/#codex128/Renthyl)

Renthyl is a modular, code-first, and completely customizable rendering graph.

![VXGI-demo](resources/VXGI-demo.png)

## Get Started

Using basic Renthyl rendering graphs are easy. First add Renthyl to the Gradle build script.

```groovy
repositories {
    maven { url "https://jitpack.io" }
}
dependencies {
    implementation "com.github.codex128.Renthyl:RenthylCore:2.0.1-alpha"
}
```

Create a FrameGraph object, attach tasks to render to it, and render the graph.
Pass the current frame time in seconds and the shared `GlobalAttributes`
(import `codex.renthyl.BasicGlobalAttributes`):

```java
FrameGraph fg = new FrameGraph();
fg.addTask(new MyCustomTask());
fg.addTask(new SomeOtherTask());
fg.render(new BasicGlobalAttributes(), timePerFrame);
```

### Use with JMonkeyEngine 3.8+

Renthyl was originally a render pipeline implementation for [JMonkeyEngine](https://jmonkeyengine.org/). With Renthyl 2.0, the core library no longer depends on JMonkeyEngine. Engine-specific implementations have been moved to the "RenthylJme" subproject.

```groovy
dependencies {
    implementation "com.github.codex128.Renthyl:RenthylJme:2.0.1-alpha"
}
```

The following code creates and registers a very simple forward-style FrameGraph in a JMonkeyEngine 3.8+ application.

```java
// create and attach framegraph
JmeFrameGraph fg = new JmeFrameGraph(assetManager);
viewPort.setPipeline(fg);

// create resource allocator
ResourceAllocationState allocator = new ResourceAllocationState();
stateManager.attach(allocator);

// create tasks
SceneEnqueuePass enqueue = SceneEnqueuePass.withSingleQueue();
GeometryPass geometry = new GeometryPass(allocator);
OutputPass out = fg.addTask(new OutputPass());

// share task resources
geometry.getGeometry().addMapSource(enqueue.getQueues());
out.getColor().setUpstream(geometry.getOutColor());
out.getDepth().setUpstream(geometry.getOutDepth());
```

It's not recommended to use this particular FrameGraph setup for serious rendering, as it doesn't handle queue buckets properly or render controls.

## Tutorials

* [How to use Renthyl](Wiki/2_0_0/HowToUseRenthyl.md)
* [Renthyl with JME](Wiki/2_0_0/RenthylWithJme.md)
