package com.jioremote.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TvConnectionStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun restoresLastSavedEndpointFromDisk() = runTest {
        val file = folder.root.resolve("connection.preferences_pb")
        val firstScope = CoroutineScope(backgroundScope.coroutineContext.minusKey(Job))
        val first = TvConnectionStore(PreferenceDataStoreFactory.create(scope = firstScope) { file })
        val latest = TvConnection("192.168.1.82", "37841")
        try {
            assertEquals(TvConnection(), first.load())
            first.save(TvConnection("10.0.0.8", "5555"))
            first.save(latest)
        } finally {
            firstScope.coroutineContext.job.cancelAndJoin()
        }

        val reopened = TvConnectionStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
        assertEquals(latest, reopened.load())
    }
}
