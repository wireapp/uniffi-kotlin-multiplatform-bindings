import callbacks.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.expect

class CallbacksTest {

    class KotlinGetters : ForeignGetters {
        override fun getBool(v: Boolean, argumentTwo: Boolean): Boolean = v xor argumentTwo
        override fun getString(v: String, arg2: Boolean): String {
            if (v == "bad-argument") {
                throw SimpleException.BadArgument("bad argument")
            }
            if (v == "unexpected-error") {
                throw RuntimeException("something failed")
            }
            return if (arg2) "1234567890123" else v
        }

        override fun getOption(v: String?, arg2: Boolean): String? {
            if (v == "bad-argument") {
                throw ComplexException.ReallyBadArgument(20)
            }
            if (v == "unexpected-error") {
                throw RuntimeException("something failed")
            }
            return if (arg2) v?.uppercase() else v
        }

        override fun getList(v: List<Int>, arg2: Boolean): List<Int> = if (arg2) v else listOf()
    }

    @Test
    fun callbackAsArgument() {
        val rustGetters = RustGetters()

        val callback = KotlinGetters()
        listOf(true, false).forEach { v ->
            val flag = true
            val expected = callback.getBool(v, flag)
            val observed = rustGetters.getBool(callback, v, flag)
            expected shouldBe observed
        }

        listOf(listOf(1, 2), listOf(0, 1)).forEach { v ->
            val flag = true
            val expected = callback.getList(v, flag)
            val observed = rustGetters.getList(callback, v, flag)
            expected shouldBe observed
        }

        listOf("Hello", "world").forEach { v ->
            val flag = true
            val expected = callback.getString(v, flag)
            val observed = rustGetters.getString(callback, v, flag)
            expected shouldBe observed
        }

        listOf("Some", null).forEach { v ->
            val flag = false
            val expected = callback.getOption(v, flag)
            val observed = rustGetters.getOption(callback, v, flag)
            observed shouldBe expected
        }

        rustGetters.getStringOptionalCallback(callback, "TestString", false) shouldBe "TestString"
        rustGetters.getStringOptionalCallback(null, "TestString", false) shouldBe null

        shouldThrow<SimpleException.BadArgument> {
            rustGetters.getString(callback, "bad-argument", true)
        }
        shouldThrow<SimpleException.UnexpectedException> {
            rustGetters.getString(callback, "unexpected-error", true)
        }

        shouldThrow<ComplexException.ReallyBadArgument> {
            rustGetters.getOption(callback, "bad-argument", true)
        }.also {
            it.code shouldBe 20
        }

        shouldThrow<ComplexException.UnexpectedErrorWithReason> {
            rustGetters.getOption(callback, "unexpected-error", true)
        }.also {
            it.reason shouldBe RuntimeException("something failed").toString()
        }
        rustGetters.destroy()
    }

    class StoredKotlinStringifier : StoredForeignStringifier {
        override fun fromSimpleType(value: Int): String = "kotlin: $value"

        // We don't test this, but we're checking that the arg type is included in the minimal list of types used
        // in the UDL.
        // If this doesn't compile, then look at TypeResolver.
        override fun fromComplexType(values: List<Double?>?): String = "kotlin: $values"
    }

    @Test
    fun callbackAsConstructorArgument() {
        val kotlinStringifier = StoredKotlinStringifier()
        val rustStringifier = RustStringifier(kotlinStringifier)
        listOf(1, 2).forEach { v ->
            val expected = kotlinStringifier.fromSimpleType(v)
            val observed = rustStringifier.fromSimpleType(v)
            expected shouldBe observed
        }
        rustStringifier.destroy()
    }

    @Test
    fun callbackClassTest() {
        val result = runBlocking { doSomething(2u) }
        println(result)
    }

    @Test
    fun doMoreTest() {
        val result = runBlocking { doAWholeLotMore(4u) }
        println(result)
    }

    @Test
    fun callbackReturningVoid() {
        runBlocking {
            val secondAnswer = meowAsync(41u)
            secondAnswer shouldBe 41u
            println("secondAnswer: ${secondAnswer}")
        }

        val answer = 42uL

        val callback = object : VoidCallback {
            public var answer: ULong? = null;

            override fun callBack(newValue: ULong) {
                this.answer = newValue
            }
        }

        VoidCallbackProcessor(answer).use {
            it.process(callback)
        }

        callback.answer shouldBe answer
    }

    @Test
    fun callBackReturningResultOfVoid() {
        // FIXME Running the commented out block below before the rest of the test will make it fail on the JVM
        //       Possibly, there is something wrong with the lifetime of the callbacks in use as JNA prints the following
        //       > JNA: callback object has been garbage collected
        //       > JNA: callback object has been garbage collected
        // Consequently, if you use the "no-op GC" the tests pass
        //     tasks.withType<KotlinJvmTest>().configureEach {
        //        this.jvmArgs = listOf("-XX:+UnlockExperimentalVMOptions", "-XX:+UseEpsilonGC")
        //    }
//        runBlocking {
//            val secondAnswer = meowAsync(41u)
//            secondAnswer shouldBe 41u
//            println("secondAnswer: ${secondAnswer}")
//        }

        val answer = 42uL
        val errorMessage = "That feels off"

        val throwingCallback = object : VoidCallbackWithError {
            public var answer: ULong? = null;


            override fun callBack(newValue: ULong) {
                if (newValue != 42uL) {
                    throw ComplexException.UnexpectedErrorWithReason(errorMessage)
                }
                this.answer = answer
            }
        }

        VoidCallbackWithErrorProcessor(throwingCallback).use {
            shouldThrow<ComplexException> { it.process(7uL) }
                .message shouldInclude errorMessage

            it.process(answer)
        }

        throwingCallback.answer shouldBe answer
    }

    @Test
    fun asyncTest() {
        val answer = runBlocking {
            meowAsync(42u)
        }
        answer shouldBe 42u
        println("meow")
    }

    @Test
    fun testAsyncFunctionWithResultReturnType() {
        val answer = runBlocking { asyncError(42u) }
        assertEquals(42u, answer)

        shouldThrow<SimpleException> {
            runBlocking {
                asyncError(3u)
            }
        }
    }

    @Test
    fun testAsyncFunctionWithUnitReturnType() {
        runBlocking { asyncUnit(42u) }
        runBlocking { asyncNoInputParam() }

        Caller().use { caller ->
            assertEquals(41u, caller.getInner())
            runBlocking { caller.interiorMutation() }
            assertEquals(42u, caller.getInner())
        }
    }

    @Test
    fun procMacroTest() {
        val answer = cthulu(41u, 1u)
        answer shouldBe 42u

        runBlocking {
            val secondAnswer = meowAsync(41u)
            secondAnswer shouldBe 41u
            println("secondAnswer: ${secondAnswer}")
        }

        val anotherOne = cthulu(32u, 10u)
        anotherOne shouldBe answer

        runBlocking {
            val secondAnswer = meowAsync(41u)
            secondAnswer shouldBe 41u
            println("secondAnswer: ${secondAnswer}")
        }

        runBlocking {
            val secondAnswer = meowAsync(41u)
            secondAnswer shouldBe 41u
            println("secondAnswer: ${secondAnswer}")
        }

        runBlocking {
            doNothing(1u)
        }

        runBlocking { doSomething(7u) } shouldBe 42u
    }


    @Test
    fun asyncMethodTest() {
        val someObject = object {
            val someList: MutableList<String> = mutableListOf()

            fun add(times: Int) {
                someList.addAll(generateSequence { "Meow" }.take(times))
            }
        }
        runBlocking {
            val secondAnswer = meowAsync(41u)
            assertEquals(41u, secondAnswer)
            someObject.add(1024)
            println("secondAnswer: ${secondAnswer}")
        }

        Caller().use {
            val answer = runBlocking { it.call() }
            assertEquals(42u, answer)
        }

        runBlocking {
            val secondAnswer = meowAsync(41u)
            assertEquals(secondAnswer, 41u)
            println("secondAnswer: ${secondAnswer}")
        }

        Caller().use {
            val answer = runBlocking { it.call() }
            assertEquals(42u, answer)
        }

        val rustGetters = RustGetters()
        val callback = KotlinGetters()
        listOf(true, false).forEach { v ->
            val flag = true
            val observed = rustGetters.getBool(callback, v, flag)
            val expected = callback.getBool(v, flag)
            expected shouldBe observed
        }
    }
}
