# xctraceprof
![GitHub](https://img.shields.io/github/license/fzhinkin/xctraceprof)
![Maven Central](https://img.shields.io/maven-central/v/io.github.fzhinkin/xctraceprof)

JMH profilers based on "xctrace" utility (shipped with XCode Instruments).

The goal of the project is to extend and stabilize profiler features, and then, hopefully, contribute it to JMH.

## Profilers
The library provides two profilers:
- `org.openjdk.jmh.profile.XCTraceAsmProfiler`, a profiler using `xctrace` to sample the process
during benchmark's execution and then annotates hot regions of the assembly.
It's the `perfasm` profiler replacement on macOS.
- `org.openjdk.jmh.profile.XCTraceNormProfiler`, a profiler using `xctrace` to aggregate hardware
performance counters during benchmark's execution.
It's the `perfnorm` profiler replacement on macOS.

Both profilers support `template` parameter specifying XCode Instruments template's name or path to it.
For `XCTraceAsmProfiler` that parameter is optional and `CPU Profiler` will be used by default.

For `XCTraceNormProfiler` that parameter is mandatory.

### Output example
Output examples were gathered for [JMH sample #35](https://github.com/openjdk/jmh/blob/362d6579e007f0241f05c1305f0b269fcc2cc27a/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java#L362).

<details>
<summary>XCTraceAsmProfiler output example</summary>

```text
Secondary result "org.openjdk.jmh.samples.JMHSample_35_Profilers.Atomic.test:asm":
PrintAssembly processed: 121645 total address lines.
Perf output processed (skipped 16,116 seconds):
 Column 1: sampled_pc (7683 events)

Hottest code regions (>10,00% "sampled_pc" events):
 Event counts are percents of total event count.

....[Hottest Region 1]..............................................................................
c2, level 4, org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub, version 6, compile id 582 

                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@20 (line 236)
             0x000000011904a889:   movzbl 0x98(%r14),%r11d             ; implicit exception: dispatches to 0x000000011904a958
                                                                       ;*getfield isDone {reexecute=0 rethrow=0 return_oop=0}
                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@30 (line 238)
             0x000000011904a891:   test   %r11d,%r11d
             0x000000011904a894:   jne    0x000000011904a944           ;*ifeq {reexecute=0 rethrow=0 return_oop=0}
                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@33 (line 238)
             0x000000011904a89a:   mov    $0x1,%ebp
             0x000000011904a89f:   nop                                 ;*aload_1 {reexecute=0 rethrow=0 return_oop=0}
                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@36 (line 239)
   3,42%  ↗  0x000000011904a8a0:   mov    0xc(%r8),%r11d               ;*getfield n {reexecute=0 rethrow=0 return_oop=0}
          │                                                            ; - org.openjdk.jmh.samples.JMHSample_35_Profilers$Atomic::test@1 (line 342)
          │                                                            ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@17 (line 236)
   0,04%  │  0x000000011904a8a4:   test   %r11d,%r11d
          │  0x000000011904a8a7:   je     0x000000011904a912
          │  0x000000011904a8ad:   mov    $0x1,%r10d
          │  0x000000011904a8b3:   lock xadd %r10,0x10(%r12,%r11,8)    ;*invokevirtual getAndAddLong {reexecute=0 rethrow=0 return_oop=0}
          │                                                            ; - java.util.concurrent.atomic.AtomicLong::incrementAndGet@8 (line 233)
          │                                                            ; - org.openjdk.jmh.samples.JMHSample_35_Profilers$Atomic::test@4 (line 342)
          │                                                            ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@17 (line 236)
  90,47%  │  0x000000011904a8ba:   movzbl 0x98(%r14),%r11d             ;*getfield isDone {reexecute=0 rethrow=0 return_oop=0}
          │                                                            ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@30 (line 238)
   0,16%  │  0x000000011904a8c2:   add    $0x1,%rbp                    ;*ladd {reexecute=0 rethrow=0 return_oop=0}
          │                                                            ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@26 (line 237)
   3,50%  │  0x000000011904a8c6:   add    $0x1,%r10                    ;*getfield isDone {reexecute=0 rethrow=0 return_oop=0}
          │                                                            ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@30 (line 238)
   0,03%  │  0x000000011904a8ca:   mov    0x350(%r15),%r10             ; ImmutableOopMap {r8=Oop rbx=Oop r13=Oop r14=Oop }
          │                                                            ;*ifeq {reexecute=1 rethrow=0 return_oop=0}
          │                                                            ; - (reexecute) org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@33 (line 238)
   0,01%  │  0x000000011904a8d1:   test   %eax,(%r10)                  ;   {poll}
   0,04%  │  0x000000011904a8d4:   test   %r11d,%r11d
          ╰  0x000000011904a8d7:   je     0x000000011904a8a0           ;*aload_1 {reexecute=0 rethrow=0 return_oop=0}
                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@36 (line 239)
             0x000000011904a8d9:   nopl   0x0(%rax)
             0x000000011904a8e0:   movabs $0x1092488f0,%r10
             0x000000011904a8ea:   call   *%r10                        ;*invokestatic nanoTime {reexecute=0 rethrow=0 return_oop=0}
                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@37 (line 239)
             0x000000011904a8ed:   mov    %rax,0x30(%rbx)              ;*putfield stopTime {reexecute=0 rethrow=0 return_oop=0}
                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@40 (line 239)
             0x000000011904a8f1:   mov    %r12,0x20(%rbx)              ;*putfield realTime {reexecute=0 rethrow=0 return_oop=0}
                                                                       ; - org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub@46 (line 240)
....................................................................................................
  97,67%  <total for region 1>

....[Hottest Regions]...............................................................................
  97,67%               c2, level 4  org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub, version 6, compile id 582 
   0,23%                      dyld  dyld3::MachOLoaded::findClosestSymbol(unsigned long long, char const**, unsigned long long*) const 
   0,05%    libsystem_kernel.dylib  __error 
   0,05%    libsystem_kernel.dylib  write 
   0,04%                            <unknown> 
   0,04%                            <unknown> 
   0,04%                            <unknown> 
   0,04%                            <unknown> 
   0,04%         libsystem_c.dylib  flockfile 
   0,04%   libsystem_pthread.dylib  _pthread_mutex_firstfit_unlock_slow 
   0,03%                            <unknown> 
   0,03%                            <unknown> 
   0,03%                            <unknown> 
   0,03%                            <unknown> 
   0,03%                            <unknown> 
   0,03%                            <unknown> 
   0,03%               interpreter  invokeinterface  185 invokeinterface  
   0,03%         libsystem_c.dylib  __vfprintf 
   0,03%         libsystem_c.dylib  __vfprintf 
   0,03%         libsystem_c.dylib  __sfvwrite 
   1,48%  <...other 107 warm regions...>
....................................................................................................
  99,99%  <totals>

....[Hottest Methods (after inlining)]..............................................................
  97,67%               c2, level 4  org.openjdk.jmh.samples.jmh_generated.JMHSample_35_Profilers_Atomic_test_jmhTest::test_avgt_jmhStub, version 6, compile id 582 
   1,20%                            <unknown> 
   0,23%                      dyld  dyld3::MachOLoaded::findClosestSymbol(unsigned long long, char const**, unsigned long long*) const 
   0,10%         libsystem_c.dylib  __vfprintf 
   0,05%    libsystem_kernel.dylib  __error 
   0,05%   libsystem_pthread.dylib  _pthread_mutex_firstfit_unlock_slow 
   0,05%    libsystem_kernel.dylib  write 
   0,04%         libsystem_c.dylib  flockfile 
   0,03%  libsystem_platform.dylib  _platform_strlen 
   0,03%               interpreter  return entry points  
   0,03%  libsystem_platform.dylib  _platform_strchr$VARIANT$Haswell 
   0,03%  libsystem_platform.dylib  _platform_memmove$VARIANT$Haswell 
   0,03%    libsystem_kernel.dylib  _kernelrpc_mach_port_deallocate_trap 
   0,03%         libsystem_c.dylib  __ultoa 
   0,03%               interpreter  invokeinterface  185 invokeinterface  
   0,03%    libsystem_kernel.dylib  mach_port_deallocate 
   0,03%         libsystem_c.dylib  __sfvwrite 
   0,01%   libsystem_pthread.dylib  pthread_mutex_unlock 
   0,01%         libsystem_c.dylib  DYLD-STUB$$__error 
   0,01%    libsystem_malloc.dylib  small_malloc_should_clear 
   0,31%  <...other 24 warm methods...>
....................................................................................................
  99,99%  <totals>

....[Distribution by Source]........................................................................
  97,67%               c2, level 4
   1,20%                          
   0,33%         libsystem_c.dylib
   0,23%                      dyld
   0,18%    libsystem_kernel.dylib
   0,14%               interpreter
   0,10%  libsystem_platform.dylib
   0,09%   libsystem_pthread.dylib
   0,03%    libsystem_malloc.dylib
   0,01%               c1, level 3
....................................................................................................
  99,99%  <totals>
```

</details>

<details>
<summary>XCTraceNormProfiler output example</summary>

```text
Benchmark                                                   Mode  Cnt    Score     Error                       Units
JMHSample_35_Profilers.Atomic.test                          avgt   15  148,414 ±  15,503                       ns/op
JMHSample_35_Profilers.Atomic.test:CORE_ACTIVE_CYCLE        avgt    3  357,637 ± 819,859                        #/op
JMHSample_35_Profilers.Atomic.test:CPI                      avgt    3   34,059 ±  78,102  CORE_ACTIVE_CYCLE/INST_ALL
JMHSample_35_Profilers.Atomic.test:INST_ALL                 avgt    3   10,505 ±   5,342                        #/op
JMHSample_35_Profilers.Atomic.test:IPC                      avgt    3    0,030 ±   0,067  INST_ALL/CORE_ACTIVE_CYCLE
JMHSample_35_Profilers.Atomic.test:L1D_CACHE_MISS_LD        avgt    3    0,617 ±   2,097                        #/op
JMHSample_35_Profilers.Atomic.test:MEM_LOAD_RETIRED.L1_HIT  avgt    3    3,631 ±   2,268                        #/op
```

</details>

## Prerequisites
Both profilers require `xctrace` utility available on your device running under macOS.
You can check if it exists by opening a terminal and running the following command:
```bash
xctrace version
```
If the utility is available, then you'll see something like `xctrace version 14.3.1 (14E300c)`. 
Otherwise, you need to install XCode.

## How to use

### Configure a project using JMH framework

Refer to [JMH docs](https://github.com/openjdk/jmh/blob/master/README.md) for details on setting up a project.

### Include library to your project's dependencies

For Gradle-based projects:
```kotlin
dependencies {
    implementation("io.github.fzhinkin:xctraceprof:0.0.2")
}
```

For Maven-based projects:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.fzhinkin</groupId>
        <artifactId>xctraceprof</artifactId>
        <version>0.0.2</version>
    </dependency>
</dependencies>
```

###

### Specify profilers when running JMH benchmarks
```bash
java <jvm args> -jar benchmarks.jar <benchmark options>  -prof 'org.openjdk.jmh.profile.XCTraceAsmProfiler'
java <jvm args> -jar benchmarks.jar <benchmark options>  -prof 'org.openjdk.jmh.profile.XCTraceAsmProfiler:template=Time Profiler'
java <jvm args> -jar benchmarks.jar <benchmark options>  -prof 'org.openjdk.jmh.profile.XCTraceNormProfiler:template=YourTemplate'
```

## Configuring your own template
You can configure your own profiling template in Instruments.
Currently, profilers support only templates that use one of the following instruments:
- `CPU Profiler`
- `Time Profiler`
- `CPU Counters`

There are not so many recording settings for `CPU Profiler` and `Time Profiler`, so you can
use these templates with `XCTraceAsmProfiler` as it is. However, if you want to profile benchmarks
using a hardware performance counter other that cycles count or willing to use `XCTraceNormProfiler` then
you need to configure a template using `CPU Counters` instrument and change recording settings
to sample by particular event, or to sample specific hardware performance counter values. 

Here's a small demo showing how to create a template to sample cycles and instructions on Intel-based MacBook:
![Configure template](create-template.gif)

## Licence

Distributed under the GNU GPL V2 License. See [LICENSE](LICENSE) for more information.


