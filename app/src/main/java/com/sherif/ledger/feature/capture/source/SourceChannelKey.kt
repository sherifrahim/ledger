package com.sherif.ledger.feature.capture.source

import dagger.MapKey

/** Dagger map key so adapters register into a Map<SourceChannel, SourceAdapter>. */
@MapKey
annotation class SourceChannelKey(val value: SourceChannel)
