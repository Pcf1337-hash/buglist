package com.buglist.presentation.person_detail

import com.buglist.domain.model.PairingLink
import com.buglist.domain.model.SyncRole

enum class CouplingStep { CONNECT, ROLE, VERIFY, DONE }

fun couplingStep(link: PairingLink?, synced: Boolean, busy: Boolean): CouplingStep = when {
    link == null || !link.peerSeen -> CouplingStep.CONNECT
    link.role == SyncRole.PENDING || link.wantedHost == null || link.peerHost == null ||
        link.wantedHost == link.peerHost -> CouplingStep.ROLE
    !synced || busy -> CouplingStep.VERIFY
    else -> CouplingStep.DONE
}
