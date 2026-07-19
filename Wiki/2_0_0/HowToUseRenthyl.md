
# Introduction to Renthyl

Renthyl FrameGraphs are composed of interconnected [Renderables](). At render, all Renderables are staged into a [RenderQueue](), and then rendered in order. The following class is a simple Renderable implementation using [AbstractTask]() that outputs some text when rendered.

```java
public class PrintToConsole extends AbstractTask {
    
    private final String text;
    
    public PrintToConsole(String text) {
        this.text = text;
    }
    
    @Override
    protected void renderTask() {
        System.out.println("Rendering text: " + text);
    }
    
}
```

The `renderTask` method is, of course, called when the task is rendered by the RenderingQueue. To see this class in action, create a FrameGraph, attach a PrintToConsole instance to it, and render the FrameGraph.

```java
FrameGraph fg = new FrameGraph();
fg.add(new PrintToConsole("Hello World!"));
fg.render();
```

Running this gives the following output:

```text
Rendering text: Hello World!
```

Multiple PrintToConsole tasks can be added to the same FrameGraph.

```java
FrameGraph fg = new FrameGraph();
fg.add(new PrintToConsole("Hello World!"));
fg.add(new PrintToConsole("First Renthyl program."));
PrintToConsole home = fg.addTask(new PrintToConsole("Home sweet home"));
fg.render();
```

*Note: `addTask` adds and returns the task, rather than just adding it.*

```text
Rendering text: Hello World!
Rendering text: First Renthyl program.
Rendering text: Home sweet home
```

Now suppose we have another Renderable implementation that performs some sort of calculation at render time (in this case, concatenating two strings).

```java
public class ConcatText extends AbstractTask {
    
    private final String text1, text2;
    
    public ConcatText(String text1, String text2) {
        this.text1 = text1;
        this.text2 = text2;
    }
    
    @Override
    protected void renderTask() {
        String result = text1 + '/' + text2;
    }
    
}
```

We could use a `println` inside ConcatText to view the results of the concatenation, but remember, we have PrintToConsole that already does that job! It would be more optimal to reuse PrintToConsole to view the result of ConcatText.

This is where the Socket interface comes into play. Sockets, in there simplest sense, share resources with and receive resources from other Sockets. We can use them to communicate the result from ConcatText to PrintToConsole. First add a Socket (in this case an ArgumentSocket) to ConcatText to represent the concatenation result.

```java
public final ArgumentSocket<String> resultSocket = new ArgumentSocket<>(this);
```

All sockets in a task *must* be properly managed. AbstractTask will automatically manage sockets, but they must be registered first.

```java
public ConcatText(String text1, String text2) {
    ...
    addSocket(resultSocket);
}
```

In this particular case, we chose to use ArgumentSocket, which explicitly contains a definite value. In the render method, we will set the ArgumentSocket's value to the concatenation result so it can be shared.

```java
@Override
protected void renderTask() {
    String result = text1 + '/' + text2;
    resultSocket.setValue(result);
}
```

Now that ConcatText is set up to share its result, we must now configure PrintToConsole to receive that result. Replace the `text` field with a TransitiveSocket.

```java
//private final String text;
public final TransitiveSocket<String> textSocket = new TransitiveSocket<>(this);
```

We're using a TransitiveSocket because (for now) because we are simply "transfering" a value from somewhere to here. Remember to register the socket so it will be properly managed.

```java
public PrintToConsole(/*String text*/) {
    //this.text = text;
    addSocket(textSocket);
}
```

During render, use `textSocket` to fetch the result using `acquire` and print it. Acquire may only be called during `renderTask` (not during any other rendering step).

```java
@Override
protected void renderTask() {
    //System.out.println("Rendering text: " + text);
    String result = textSocket.acquire();
    System.out.println("Rendering text: " + result);
}
```

Adjust the FrameGraph setup to reflect our changes.

```java
FrameGraph fg = new FrameGraph();
ConcatText concat = fg.add(new ConcatText("Hello", "World"));
PrintToConsole print = fg.add(new PrintToConsole());
fg.render();
```

Running the program at this point prints absolutely nothing. That is because we never told PrintToConsole's socket what other socket it should fetch the result from. We need to set the upstream socket of PrintToConsole's `textSocket` socket to ConcatText's `resultSocket` before rendering the graph.

```java
...
print.textSocket.setUpstream(concat.resultSocket);
fg.render();
```

You now should see this output:

```text
Rendering text: Hello/World
```

Here is the full code up to this point:

```java
public class Main {
    public static void main(String[] args) {
        FrameGraph fg = new FrameGraph();
        ConcatText concat = fg.add(new ConcatText("Hello", "World"));
        PrintToConsole print = fg.add(new PrintToConsole());
        print.textSocket.setUpstream(concat.resultSocket);
        fg.render();
    }
}

public class ConcatText extends AbstractTask {

    private final String text1, text2;
    public final ArgumentSocket<String> resultSocket = new ArgumentSocket<>(this);

    public ConcatText(String text1, String text2) {
        this.text1 = text1;
        this.text2 = text2;
        addSocket(resultSocket);
    }

    @Override
    protected void renderTask() {
        String result = text1 + '/' + text2;
        resultSocket.setValue(result);
    }

}

public class PrintToConsole extends AbstractTask {

    public final TransitiveSocket<String> textSocket = new TransitiveSocket<>(this);

    public PrintToConsole(String text) {
        this.text = text;
        addSocket(textSocket);
    }

    @Override
    protected void renderTask() {
        System.out.println("Rendering text: " + textSocket.acquire());
    }

}
```

Now if we don't view the result of ConcatText, it would be best if ConcatText never got run in the first place. After all, it would be doing work that is ultimately unused. The FrameGraph is able to do this automatically.

First add a `println` to ConcatText's render so we know whether its actually running or not.

```java
@Override
protected void renderTask() {
    ...
    System.out.println("Concatenating text...");
}
```

Now disconnect PrintToConsole from ConcatText.

```java
//print.textSocket.setUpstream(concat.resultSocket);
```

If all works as expected, ConcatText shouldn't run at all, because its result is useless. However, ConcatText is clearly still getting rendered when we run the program (even though we aren't using ConcatText's result).

```text
Concatenating text...
Rendering text: null
```

This is because we directly attached ConcatText to the FrameGraph via `add`. All tasks directly attached are guaranteed to be rendered. We can easily fix this by simply not adding ConcatText to the FrameGraph.

```java
//ConcatText concat = fg.add(new ConcatText("Hello", "World"));
ConcatText concat = new ConcatText("Hello", "World");
```

Running now will give the correct result:

```text
Rendering text: null
```

It may seem now that ConcatText cannot be rendered even if we wanted it to, because the FrameGraph doesn't realize ConcatText exists at all. That is true if ConcatText is not connected to PrintToConsole. If we reconnect PrintToConsole and ConcatText, the FrameGraph will render ConcatText again.

```java
print.textSocket.setUpstream(concat.resultSocket);
```
```text
Concatenating text...
Rendering text: Hello/World
```

Note that if we also don't `add` PrintToConsole, nothing gets rendered at all. Only tasks directly added to a FrameGraph *or* connected (either directly or through a chain of tasks) to something directly added to a FrameGraph get rendered with that FrameGraph.

Right now, PrintToConsole will print `null` if nothing is connected with it. There may be cases where we wish to print a constant we provide directly rather than needing to provide an input. We can do this by using an ArgumentSocket instead of a TransitiveSocket in PrintToConsole.

```java
//public final TransitiveSocket<String> textSocket = new TransitiveSocket<>(this);
public final ArgumentSocket<String> textSocket = new ArgumentSocket<>(this);
```

ArgumentSocket is itself an extension of TransitiveSocket, so we don't need to modify any existing code to support it. ArgumentSocket accepts a value directly, and will return that value on `acquire` if the socket has no upstream socket. In this sense, the direct value is a default value.

Set the socket's value:

```java
print.textSocket.setValue("default value");
```

To test it, disconnect PrintToConsole from ConcatText.

```java
//print.textSocket.setUpstream(concat.resultSocket);
```
```text
Rendering text: default value
```

Since PrintToConsole no longer pulls from ConcatText, ConcatText does not get rendered, and PrintToConsole must use the default value of `textSocket`.

> All tasks should be designed to never mutate input resources. For example, this class is illegal because it modifies the incoming integer array from `input`.
> ```java
> public class IllegalTask extends AbstractTask {
> 
>     public final TransitiveSocket<int[]> input = new TransitiveSocket(this);
> 
>     public IllegalTask() {
>         addSocket(input);
>     }
>     
>     @Override
>     protected void renderTask() {
>         int[] inArray = input.acquire();
>         inArray[0] = 56;
>     }
> 
> }
> ```
> This is because other tasks may be using that resource, and by changing it, you break synchronization and risk concurrent modification. In general, mutating inputs will give uncertain results. Outputs, on the other hand, are perfectly fine to mutate.

## FrameGraph Stages

Now that we have some grasp on how to work with FrameGraphs, let's look a little bit at what is going on under the hood. FrameGraph rendering is divided into 4 seperate stages.

* Staging
* Preparing
* Rendering
* Resetting

Staging is the most comlicated of the bunch. It is responsible for flattening the graph structure into a queue. This is done, briefly, by performing depth-first, post-iteration, traversal of the graph starting from the tasks directly attached to the FrameGraph (via `add`). Sockets play a huge role in the traversal, as they represent connections between tasks. This process automatically "culls" tasks that aren't used.

Preparing is performed much more simply, as all tasks to be prepared have been queued. It calculates when and how values (or resources) are passed among sockets.

Rendering we have already discussed to some degree. Not mentioned is that all resources used from sockets must be properly released after use. AbstractTask does this automatically for registered sockets.

Reset performs no FrameGraph-specific operations. It more exists as a convenient step after rendering to reset whatever requires resetting.

## Resource Allocation

Some resources (such as images) may benefit from special handling to avoid using too many of them at once. The premise is that particularly expensive resources (again, like images) can be reused in certain situations. Renthyl provides a basic system to do this, centered around the ResourceAllocator interface.

```java
public interface ResourceAllocator <T extends ResourceWrapper> {
    
    T allocate(ResourceDef def, int start, int end);

}
```

ResourceAllocator returns a ResourceWrapper containing the allocated resource. The argument `def` is an object describing the properties of the returned resource. `start` and `end` define the estimated timeframe in which the returned resource will be actively used by the caller.

The returned wrapper is like a mini ResourceAllocator in some respects. It controls access to the underlying resource by requiring that it first by acquired, and then released afterward. Tasks typically save the returned wrapper so that the task can use the exact same resource during the next render.

The returned wrapper is already acquired when returned from the ResourceAllocator.

```java
public interface ResourceDef <T> extends Disposer<T> {
    
    T createResource();
    
    Float evaluateResource(Object resource);
    
    T conformResource(Object resource);
    
}
```

ResourceDef selects the best existing resource to allocate using `evaluateResource` (lowest score wins) and `conformResource` (cast to target type). If no existing resources are deemed suitable, a new resource is created with `createResource`.

> While it isn't required to use this system for resource allocation, a fair amount of Renthyl infrastructure is built around it that would need to be rewritten. If possible, do use a ResourceAllocator of some sort.

### Allocation for Rendering

Suppose we have a GenerateIntegerArray task that generates an array of integers. We want to use a ResourceAllocator to allocate the array since it would be best to reuse the array if possible rather than create new ones continually.

```java
public class GenerateIntegerArray extends AbstractTask {
    
    public GenerateIntegerArray() {
        
    }
    
    @Override
    protected void renderTask() {
        int[] array = ...
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
    }
    
}
```

First some sort of ResourceAllocator is necessary to allocate the int array. It's best practice to have ResourceAllocators have as many tasks requesting resources from them as possible, so we will require a ResourceAllocator as a constructor argument.

```java
private final ResourceAllocator allocator;

public GenerateIntegerArray(ResourceAllocator allocator) {
    this.allocator = allocator;
}
```

We will also need a ResourceDef to describe the array. IntArrayDef is an implementation that describes integer arrays specifically.

```java
private final IntArrayDef arrayDef = new IntArrayDef();
```

We need to configure `arrayDef` to only select arrays of a certain length or larger. For this tutorial we'll stick with 10 elements.

```java
public GenerateIntegerArray(ResourceAllocator allocator) {
    ...
    arrayDef.setSize(10);
}
```

Now, during render, allocate an integer array from `allocator` using `arrayDef`. For the `start` and `end` arguments, we will use `getPosition()` which retrieves the position of the task within the RenderingQueue, just to be on the safe side. We could technically submit anything since allocators are built to expect garbage for those arguments, but that wouldn't be good practice.

```java
@Override
protected void renderTask() {
    ResourceWrapper wrapper = allocator.allocate(arrayDef, getPosition(), getPosition());
    ...
}
```

Now that we have the ResourceWrapper, we must extract its underlying resource for use. Wrappers don't care what resource type they carry, so we'll have to convert it ourselves. Fortunately, `arrayDef` will take care of that for use.

```java
int[] array = arrayDef.conformResource(wrapper.get());
```

After we are finished with `array`, remember to release the ResourceWrapper to indicate that we are finished with the resource. If we don't, the wrapper will assume we're still using it and block attempts to re-allocate the resource.

```java
@Override
protected void renderTask() {
    ...
    wrapper.release();
}
```

Now suppose we wish to produce the array as output of the task. We could set up an ArgumentSocket to transfer the array, and that could technically work, but there is a better socket type we can use: AllocationSocket. This socket will handle the allocation and wrappers for us, and allow other tasks to access the resource. Additionally, AllocationSocket will release the wrapper at an optimal time; something that would be quite challenging with ArgumentSocket.

First create and register an AllocationSocket. Since AllocationSocket requires a ResourceAllocator in the constructor, we must declare the socket a little differently this time.

```java
public final AllocationSocket<int[]> arraySocket;

public GenerateIntegerArray(ResourceAllocator allocator) {
    arraySocket = addSocket(new AllocationSocket<>(this, allocator, arrayDef));
    ...
}
```

Our rendering logic becomes incredibly simple.

```java
@Override
protected void renderTask() {
    int[] array = arraySocket.acquire();
    for (int i = 0; i < array.length; i++) {
        array[i] = i;
    }
}
```

Now the allocation process is much simpler, and the resource is automatically available to any other tasks. Not only that, but AllocationSocket also provides optimal `start` and `end` values for allocation.

Here is the full code for GenerateIntegerArray, with some additional code cleanup.

```java
public class GenerateIntegerArray extends AbstractTask {
    
    public final AllocationSocket<int[]> arraySocket;
    private final IntArrayDef arrayDef = new IntArrayDef();
    
    public GenerateIntegerArray(ResourceAllocator allocator) {
        arraySocket = addSocket(new AllocationSocket<>(this, allocator, arrayDef));
        arrayDef.setSize(10);
    }
    
    @Override
    protected void renderTask() {
        int[] array = arraySocket.acquire();
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
    }
    
}
```

### Managing ResourceAllocators

There are two ResourceAllocator implementations available: LongTermAllocator and ShortTermAllocator. The preferred implementation is almost always ShortTermAllocator. Very rarely is LongTermAllocator used.

```java
ShortTermAllocator allocator = new ShortTermAllocator();
```

When creating tasks like GenerateIntegerArray, simply pass `allocator` into the task's constructor.

```java
GenerateIntegerArray gen = new GenerateIntegerArray(allocator);
```

ShortTermAllocator must be updated to destroy resources that are no longer being used. This should typically happen before or after every render frame.

```java
allocator.flush();
```

When the application ends, clear the allocator to destroy all resources.

```java
allocator.clear();
```

> For JMonkeyEngine applications, it is recommended to use ResourceAllocatorState instead of ShortTermAllocator. It is an extension of ShortTermAllocator that hooks directly into the application's lifecycle to make `flush` and `clear` calls automatically.
> ```java
> ResourceAllocatorState allocator = new ResourceAllocatorState();
> stateManager.attach(allocator);
> ```

## Renderable Tasks

Renthyl provides several Renderable implementations that greatly enhance the Renthyl experience.

### Multiplexor

Multiplexor, like its digital circuit counterpart, receives multiple inputs and chooses one of them, using an index, to produce as the output. This is amazing for enabling or disabling whole sections of the render graph depending on the application context.

For example, a Multiplexor can be used to switch between printing the results from `helloWorld` and `fireIce`.

```java
FrameGraph fg = new FrameGraph();
PrintToConsole print = fg.addTask(new PrintToConsole("nothing to see"));

ConcatText helloWorld = new ConcatText("Hello", "World"));
ConcatText fireIce = new ConcatText("Fire", "Ice"));
Multiplexor mux = new Multiplexor();
```

Connect `helloWorld` and `fireIce` as inputs to `mux`. The order does matter.

```java
mux.addUpstream(helloWorld.resultSocket);
mux.addUpstream(fireIce.resultSocket);
```

The output socket of the Multiplexor is itself, as Multiplexor is a Socket, so directly reference `mux` from `print`.

```java
print.textSocket.setUpstream(mux);
```

Since the initial index of `mux` is zero, it pipes the input from `helloWorld` to `print`, and completely ignores `fireIce`. Running the program should give the following output (remember to call `render`).

```java
fg.render();
```
```text
Concatenating text...
Rendering text: Hello/World
```

If we set the index to 1, `mux` will now pipe the input from `fireIce` to `print`.

```java
mux.getIndex().setValue(1);
```

The index is stored in something called a Macro, which is basically a socket whose value can be fetched outside the render stage. Macros are covered in more detail later on.

Running the program gives this output:

```java
Concatenating text...
Rendering text: Fire/Ice
```

One critically important function of Multiplexor is that inputs that are not chosen never get staged (or rendered, by extension). Hence why only one "concatenating text..." was printed. This makes it ideal for enabling and disabling graph features without needing to completely severe and rebuild every connection every time.

Another interesting aspect of Multiplexor is if an out-of-bounds index is provided. In such a case, the Multiplexor will not pipe any inputs through, and simply return null.

```java
mux.getIndex().setValue(2);
```
```text
Rendering text: nothing to see
```

### Attribute

Attribute functions a lot like an ArgumentSocket, except it is also a Renderable. You simply give it a value and plug it into some task's input socket.

```java
FrameGraph fg = new FrameGraph();
PrintToConsole print = fg.addTask(new PrintToConsole("nothing to see"));
Attribute<String> inText = new Attribute<>("Text from an Attribute");
print.textSocket.setUpstream(inText);
fg.render();
```

Since Attribute is a Socket as well as a Renderable, `print`'s input socket can directly reference `inText`. Running this gives this output:

```text
Rendering text: Text from an Attribute
```

### SynchronizedAttribute

Sometimes the value held and shared by an Attribute should only be accessible by one task at a time in multithreaded renderings. SynchronizedAttribute is an extension of Attribute that does exactly that. It is used exactly like an Attribute is used.

### Derivative

Derivative is an abstract Renderable whose chief function is to transform its input resource into something else to produce as its output (without mutating the input, of course). Like Multiplexor and Attribute, it is both a Renderable and a Socket.

Take, for example, this case where Derivative is used to append an "!" to the end of the input string.

```java
FrameGraph fg = new FrameGraph();
Attribute<String> startText = new Attribute<>("I am happy");
PrintToConsole print = fg.addTask(new PrintToConsole("nothing to see"));

Derivative<String, String> exclaim = new Derivative<String, String>() {
    @Override
    public String apply(String input) {
        return input + '!';
    }
};

exclaim.setUpstream(startText);
print.textSocket.setUpstream(exclaim);

fg.render();
```

The Derivative, `exclaim`, transforms the string from `startText` and before giving it to `print`. Running this should produce:

```java
Rendering text: I am happy!
```

## Sockets

We have already explored using TransitiveSocket, ArgumentSocket, and AllocationSocket, but there are many more socket implementations that Renthyl provides.

### OptionalSocket

Can be enabled or disabled. When disabled, it pretends to be a normal TransitiveSocket that passes resources, but it actually doesn't do anything. Anything connected to a disabled OptionalSocket won't get staged through it. This is incredibly helpful when implementing interfaces that require a certain socket that the implementation doesn't need. In short, OptionalSocket is a placeholder.

When enabled, OptionalSocket is identical to TransitiveSocket.

### ValueSocket

Similar to ArgumentSocket, but it allows its value to be mutated by its task. It cannot be connected with any sockets. In practice, this socket is overshadowed by ArgumentSocket, as it is hardly ever useful to be able to mutate an input value without using an AllocationSocket.

### ModifyingSocket

Similar to TransitiveSocket, but it blocks its task from executing until all other tasks have completely finished with the incoming resource. Like ValueSocket, this is hardly ever useful.

## Macros

Socket values may only be safely acquired during the render step. Macros, on the other hand, are accessible at any point. Macros are useful when the value from the Macro determines the layout of the graph, since the graph's layout may only be changed during staging, and Socket values are only available during render.

[Multiplexor](#multiplexor) uses an ArgumentMacro to hold its index.

## Write Your Own

### ResourceDef

Being able to implement ResourceDef is critical for adding resource types to the system. For this tutorial, we will create an IntBufferDef for IntBuffers. We will also be using [LWJGL3](https://github.com/LWJGL/lwjgl3) to create buffers.

```java
public class IntBufferDef implements ResourceDef<IntBuffer> {
    
    private int minimumSize;
    
    public IntBufferDef(int minimumSize) {
        this.minimumSize = minimumSize;
    }
    
    @Override
    public IntBuffer createResource() {}
    
    @Override
    public Float evaluateResource(Object resource) {}
    
    @Override
    public IntBuffer conformResource(Object resource) {}
    
    @Override
    public void dispose(IntBuffer resource) {}
    
}
```

We will first implement `createResource`. This is relatively simple: create an IntBuffer with `minimumSize` for the capacity. It would technically be more efficient to create the IntBuffer with more capacity than we currently need, but we will skip that for simplicity.

```java
@Override
public IntBuffer createResource() {
    // using Lwjgl MemoryUtil for allocation
    return MemoryUtil.memAllocInt(minimumSize);
}
```

Next is `evaluateResource`, which is responsible for assigning a Float score to the resource. The resource that receives the lowest score is chosen for allocation. If a resource is assigned a score less than or equal to `0f`, it considered "perfect", and is immediately allocated (skipping further evaluations). `null` score means the resource is completely unsuitable and cannot be allocated.

For this implementation, we will stick with either `0f` for acceptance or `null` for rejection.

```java
@Override
public Float evaluateResource(Object resource) {
    if (!(resource instanceof IntBuffer)) {
        return null; // not an IntBuffer, cannot be used at all
    }
    IntBuffer buffer = (IntBuffer)resource;
    if (buffer.capacity() >= minimumSize) {
        return 0f;
    } else {
        return null; // not the correct size
    }
}
```

Notice that even though an accepted buffer's capacity is large enough, the limit of the buffer may not be. We shouldn't mutate the resource inside `evaluateResource`, but fortunately `conformResource` is provided to do exactly that.

`conformResource` is called on the resource selected for allocation, and is supposed to prepare the resource according to the definition's properties. No resource that was rejected by `evaluateResource` will be put through this method, so we don't have to check the resource's type in this case. Resources from `createResource` are not put through this method either.

```java
@Override
public IntBuffer conformResource(Object resource) {
    IntBuffer buffer = (IntBuffer)resource;
    buffer.limit(minimumSize);
    return buffer;
}
```

Finally we must implement `dispose` which is called when the ResourceAllocator decides that the resource is no longer worth keeping alive.

```java
@Override
public void dispose(IntBuffer resource) {
    // free resource memory with Lwjgl MemoryUtil
    MemoryUtil.memFree(resource);
}
```

Note that the ResourceDef that created the resource with `createResource` is the one called to dispose the resource, so it's possible for a ResourceDef to keep track of the resources it creates for the purpose of correctly disposing of them.
