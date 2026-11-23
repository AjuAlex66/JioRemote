package com.jioremote.app.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import com.jioremote.app.adb.RemoteCommands
import com.jioremote.app.data.Channel
import com.jioremote.app.data.DefaultChannel
import com.jioremote.app.data.TvConnection
import com.jioremote.app.data.TvConnectionStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewModelTest {
    @get:Rule val folder = TemporaryFolder()
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() {
        models.clear()
        Dispatchers.resetMain()
    }

    @Test fun restoresSettingsAndSavesValidEditsWithoutConnecting() = runTest(dispatcher) {
        val store = settings()
        store.save(TvConnection("10.0.0.4", "5555"))
        val vm = model(store)
        assertEquals(ConnectionStatus.Loading, vm.state.value.connection)
        runCurrent()
        assertEquals(ConnectionStatus.Disconnected, vm.state.value.connection)
        assertEquals("10.0.0.4", vm.state.value.host)

        vm.setHost(" 192.168.1.82 ")
        vm.setPort(" 37841 ")
        vm.saveConnection()
        assertEquals(RemoteOperation.Save, vm.state.value.operation)
        runCurrent()
        assertEquals(TvConnection("192.168.1.82", "37841"), store.load())
        assertEquals(ConnectionStatus.Disconnected, vm.state.value.connection)
        assertEquals("192.168.1.82", vm.state.value.host)
        assertNull(vm.state.value.operation)
    }

    @Test fun rejectsInvalidEndpointWithoutReplacingSavedSettings() = runTest(dispatcher) {
        val store = settings()
        val saved = TvConnection("192.168.1.82", "37841")
        store.save(saved)
        val vm = model(store)
        runCurrent()
        vm.setHost("8.8.8.8")
        vm.setPort("65536")
        vm.connect()
        runCurrent()

        assertNotNull(vm.state.value.hostError)
        assertNotNull(vm.state.value.portError)
        assertFalse(vm.state.value.isBusy)
        assertEquals(saved, store.load())
    }

    @Test fun serializesRemoteWorkAndSupportsExplicitDisconnect() = runTest(dispatcher) {
        val probe = CompletableDeferred<String>()
        val key = CompletableDeferred<String>()
        val commands = mutableListOf<String>()
        val vm = model(settings(), runCommand = { _, _, command ->
            commands += command
            if (command == RemoteCommands.probe) probe.await() else key.await()
        })
        runCurrent()
        vm.setHost("192.168.1.82")
        vm.connect()
        vm.connect()
        vm.setHost("10.0.0.8")
        vm.keyEvent(164)
        runCurrent()
        assertEquals(listOf(RemoteCommands.probe), commands)
        assertEquals(ConnectionStatus.Connecting, vm.state.value.connection)
        assertEquals("192.168.1.82", vm.state.value.host)

        probe.complete("jioremote-ready")
        runCurrent()
        assertTrue(vm.state.value.canSend)
        vm.keyEvent(164)
        vm.keyEvent(25)
        vm.disconnect()
        runCurrent()
        assertEquals(listOf(RemoteCommands.probe, RemoteCommands.key(164)), commands)
        assertEquals(RemoteOperation.Command, vm.state.value.operation)

        key.complete("")
        runCurrent()
        vm.disconnect()
        assertEquals(ConnectionStatus.Disconnected, vm.state.value.connection)
        assertFalse(vm.state.value.canSend)
    }

    @Test fun unsuccessfulProbeShowsErrorAndRetainsValidSettings() = runTest(dispatcher) {
        val store = settings()
        val vm = model(store, runCommand = { _, _, _ -> "unexpected" })
        runCurrent()
        vm.setHost("192.168.1.82")
        vm.connect()
        runCurrent()

        assertEquals(ConnectionStatus.Error, vm.state.value.connection)
        assertTrue(vm.state.value.notice?.isError == true)
        assertFalse(vm.state.value.isBusy)
        assertEquals(TvConnection("192.168.1.82", "5555"), store.load())
    }

    @Test fun pairingUsesTemporaryPortAndClearsCodeWithoutConnecting() = runTest(dispatcher) {
        val store = settings()
        val pairing = CompletableDeferred<Unit>()
        val vm = model(store, pairDevice = { host, port, code ->
            assertEquals("192.168.1.82", host)
            assertEquals(37451, port)
            assertEquals("012345", code)
            pairing.await()
        })
        runCurrent()
        vm.setHost("192.168.1.82")
        vm.setPairingPort("37451")
        vm.setPairingCode("12345")
        vm.pair()
        assertNotNull(vm.state.value.pairingCodeError)

        vm.setPairingCode("012345")
        vm.pair()
        runCurrent()
        assertEquals(RemoteOperation.Pair, vm.state.value.operation)
        assertEquals("", vm.state.value.pairingCode)
        pairing.complete(Unit)
        runCurrent()
        assertEquals(ConnectionStatus.Disconnected, vm.state.value.connection)
        assertFalse(vm.state.value.isBusy)
        assertEquals(TvConnection(), store.load())
    }

    @Test fun clearingViewModelCancelsInFlightConnection() = runTest(dispatcher) {
        val probe = CompletableDeferred<String>()
        val vm = model(settings(), runCommand = { _, _, _ -> probe.await() })
        runCurrent()
        vm.setHost("192.168.1.82")
        vm.connect()
        runCurrent()
        assertEquals(ConnectionStatus.Connecting, vm.state.value.connection)
        models.clear()
        runCurrent()
        assertEquals(ConnectionStatus.Disconnected, vm.state.value.connection)
        assertNull(vm.state.value.notice)
    }

    @Test fun unconfiguredChannelOpensEditorWithoutSendingACommand() = runTest(dispatcher) {
        val channel = DefaultChannel.Flowers.create()
        val saved = mutableListOf<Channel>()
        val vm = RemoteViewModel(settings(), { listOf(channel) }, { saved.addAll(it) },
            { _, _, _ -> error("Unexpected ADB command") }, { _, _, _ -> error("Unexpected pairing") })
        models.put("channels", vm)
        runCurrent()

        vm.play(channel)
        assertEquals(channel.key, vm.state.value.channelDraft?.key)
        assertEquals("", vm.state.value.channelDraft?.number)
        vm.setChannelNumber("123; exit")
        vm.saveChannel()
        assertNotNull(vm.state.value.channelDraft?.error)
        assertTrue(saved.isEmpty())

        vm.setChannelNumber("123")
        vm.saveChannel()
        assertEquals(channel.copy(number = "123"), saved.single())
        assertEquals(saved, vm.state.value.channels)
        assertNull(vm.state.value.channelDraft)
    }

    @Test fun tuningSendsOneSequenceAndIgnoresOverlappingChannelRequests() = runTest(dispatcher) {
        val release = CompletableDeferred<String>()
        val commands = mutableListOf<String>()
        val vm = model(settings(), runCommand = { _, _, command ->
            if (command == RemoteCommands.probe) "jioremote-ready" else {
                commands += command
                release.await()
            }
        })
        runCurrent()
        vm.setHost("192.168.1.82")
        vm.connect()
        runCurrent()
        val movies = DefaultChannel.AsianetMoviesHd.create()
        vm.play(movies)
        vm.play(Channel("custom:other", "Other", "100"))
        runCurrent()
        assertEquals(listOf(RemoteCommands.channel("1827")), commands)
        assertEquals(movies.key, vm.state.value.tuningChannelKey)
        release.complete("")
        runCurrent()
        assertNull(vm.state.value.tuningChannelKey)
    }

    @Test fun addEditDeleteAndReorderPersistAcrossViewModelReopen() = runTest(dispatcher) {
        var saved = listOf(DefaultChannel.Flowers.create(), DefaultChannel.AsianetMoviesHd.create())
        var saves = 0
        val store = settings()
        val vm = model(store, loadChannels = { saved }, saveChannels = { saved = it; saves++ })
        runCurrent()

        vm.openAddChannel()
        vm.setChannelName("Favourite")
        vm.setChannelNumber("0130")
        vm.saveChannel()
        val added = saved.last()
        assertEquals("0130", added.number)
        vm.openEditChannel(added)
        vm.setChannelName("Renamed")
        vm.saveChannel()
        assertEquals("Renamed", saved.last().name)
        assertEquals(added.key, saved.last().key)

        vm.beginChannelReorder()
        vm.moveChannel(added.key, saved.first().key)
        assertEquals(added.key, vm.state.value.channels.first().key)
        assertEquals(added.key, saved.last().key)
        assertEquals(2, saves)
        vm.finishChannelReorder(save = true)
        val reopened = model(store, loadChannels = { saved }, saveChannels = { saved = it })
        assertEquals(added.key, reopened.state.value.channels.first().key)

        reopened.beginChannelReorder()
        reopened.moveChannel(added.key, saved.last().key)
        reopened.finishChannelReorder(save = false)
        assertEquals(saved, reopened.state.value.channels)
        reopened.openEditChannel(saved.first())
        reopened.confirmRemove()
        reopened.cancelRemove()
        assertEquals(3, saved.size)
        reopened.confirmRemove()
        reopened.removeChannel()
        assertEquals(2, saved.size)
        assertTrue(saved.none { it.key == added.key })
    }

    private fun TestScope.settings(): TvConnectionStore {
        val file = folder.root.resolve("tv.preferences_pb")
        return TvConnectionStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
    }

    private fun model(
        store: TvConnectionStore,
        runCommand: suspend (String, Int, String) -> String = { _, _, _ -> error("Unexpected ADB command") },
        pairDevice: suspend (String, Int, String) -> Unit = { _, _, _ -> error("Unexpected pairing") },
        loadChannels: () -> List<Channel> = { emptyList() },
        saveChannels: (List<Channel>) -> Unit = {},
    ): RemoteViewModel = RemoteViewModel(store, loadChannels, saveChannels, runCommand, pairDevice).also {
        models.put("remote", it)
    }
}
