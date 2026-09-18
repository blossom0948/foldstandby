package com.blossom.foldstand.fold

import com.blossom.foldstand.domain.DualScreenStatus

enum class RawDualScreenStatus { Unsupported, Unavailable, Available, Active, Unknown }

object DualScreenStatusMapper {
    fun map(status: RawDualScreenStatus): DualScreenStatus = when (status) {
        RawDualScreenStatus.Unsupported -> DualScreenStatus.Unsupported
        RawDualScreenStatus.Unavailable -> DualScreenStatus.Unavailable
        RawDualScreenStatus.Available -> DualScreenStatus.Available
        RawDualScreenStatus.Active -> DualScreenStatus.Active
        RawDualScreenStatus.Unknown -> DualScreenStatus.Unsupported
    }
}
