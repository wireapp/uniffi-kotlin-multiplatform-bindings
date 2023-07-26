  import futures.*
  import kotlinx.coroutines.*
  import kotlin.test.*
  import kotlin.time.*
  import kotlin.time.Duration.Companion.milliseconds

  fun assertReturnsImmediately(actualTime: Duration, testName: String) {
      assertTrue(
          // increased the margin by 1.millisecond compared to upstream
          actualTime <= 5.milliseconds, "unexpected $testName time: ${actualTime.inWholeMilliseconds}ms"
      )
  }

  fun assertApproximateTime(actualTime: Duration, expectedTime: Duration, testName: String) {
      assertTrue(
          actualTime >= expectedTime && actualTime <= expectedTime + 100.milliseconds,
          "unexpected $testName time: ${actualTime.inWholeMilliseconds}ms"
      )
  }

  fun assertSmall(duration: Duration) = assertTrue(duration < 100.milliseconds)

  @OptIn(ExperimentalTime::class)
  class FuturesTest {
      init {
          // init UniFFI to get good measurements after that
          runBlocking {
              measureTime {
                  alwaysReady()
              }
          }
      }

      @Test
      fun testAlwaysReady() = runBlocking {
          val result = measureTimedValue {
              alwaysReady()
          }

          assertTrue(result.value)

          assertReturnsImmediately(result.duration, "always_ready")
      }

      @Test
      fun testVoid() = runBlocking {
          val result: TimedValue<Unit> = measureTimedValue {
              void()
          }

          assertReturnsImmediately(result.duration, "void")
      }

      @Test
      fun testSleep() = runBlocking {
          val duration = measureTime {
              sleep(200u)
          }

          assertApproximateTime(duration, 200.milliseconds, "sleep")
      }

      @Test
      fun testSequentialFutures() = runBlocking {
          val result = measureTimedValue {
              val resultAlice = sayAfter(100u, "Alice")
              val resultBob = sayAfter(200u, "Bob")

              Pair(resultAlice, resultBob)
          }

          assertEquals(Pair("Hello, Alice!", "Hello, Bob!"), result.value)
          assertApproximateTime(result.duration, 300.milliseconds, "sequential futures")
      }

      @Test
      fun testConcurrentFutures() = runBlocking {
          val result = measureTimedValue {
              val resultAlice = async { sayAfter(100u, "Alice") }
              val resultBob = async { sayAfter(200u, "Bob") }

              Pair(resultAlice.await(), resultBob.await())
          }

          assertEquals(Pair("Hello, Alice!", "Hello, Bob!"), result.value)
          assertApproximateTime(result.duration, 200.milliseconds, "concurrent futures")
      }

      @Test
      fun testAsyncMethods() = runBlocking {
          val megaphone = newMegaphone()
          val result = measureTimedValue { megaphone.sayAfter(200u, "Alice") }

          assertEquals(result.value, "HELLO, ALICE!")
          assertApproximateTime(result.duration, 200.milliseconds, "async methods")
      }

      @Test
      fun testAsyncMaybeNewMegaphone() = runBlocking {
          val megaphone = asyncMaybeNewMegaphone(true)
          assertNotNull(megaphone)

          val notMegaphone = asyncMaybeNewMegaphone(false)
          assertNull(notMegaphone)
      }

      @Test
      fun testWithTokioRuntime() = runBlocking {
          val result = measureTimedValue {
              sayAfterWithTokio(200u, "Alice")
          }

          assertEquals("Hello, Alice (with Tokio)!", result.value)
          assertApproximateTime(result.duration, 200.milliseconds, "with tokio runtime")
      }

      @Test
      fun testFalliblefunctions() = runBlocking {
          val success = measureTimedValue {
              fallibleMe(false)
          }
          assertEquals(42u, success.value)

          val successDuration = success.duration
          println("fallible function (with result): ${successDuration.inWholeMilliseconds}ms")
          assertSmall(successDuration)

          val failDuration = measureTime {
              assertFailsWith<Exception> { fallibleMe(true) }
          }
          println("fallible function (with exception): ${failDuration.inWholeMilliseconds}ms")
          assertSmall(failDuration)
      }

      @Test
      fun testFallibleMethods() = runBlocking {
          newMegaphone().use {
              val success = measureTimedValue {
                  it.fallibleMe(false)
              }

              assertEquals(42u, success.value)
              val successDuration = success.duration
              print("fallible method (with result): ${successDuration.inWholeMilliseconds}ms")
              assertSmall(successDuration)

              val failDuration = measureTime {
                  assertFailsWith<Exception> { it.fallibleMe(true) }
              }

              print("fallible method (with exception): ${failDuration.inWholeMilliseconds}ms")
              assertSmall(failDuration)
          }
      }

      @Test
      fun testFallibleStruct() {
          runBlocking {
              assertFailsWith<MyException> { fallibleStruct(true) }
          }
      }

      @Test
      fun testRecord() = runBlocking {
          val (result, duration) = measureTimedValue {
              newMyRecord("foo", 42u)
          }

          assertEquals("foo", result.a)
          assertEquals(42u, result.b)
          print("record: ${duration.inWholeMilliseconds}ms")
          assertSmall(duration)
      }

      @Test
      fun testBrokenSleep() = runBlocking {
          val duration = measureTime {
              brokenSleep(100u, 0u) // calls the waker twice immediately
              sleep(100u) // wait for a possible failure

              brokenSleep(100u, 100u) // calls the waker a second time after 1s
              sleep(200u) // wait for a possible faillure
          }

          assertApproximateTime(duration, 500.milliseconds, "broken sleep")
      }
  }
