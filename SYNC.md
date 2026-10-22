# SYNC — herkomst van de gedeelde code

Deze map is gekopieerd uit de EnigmaCast TV-repository. De TV-repository is daarna
uitgebreid met de Companion-brug (zie de fase-secties hieronder); de rest is niet
gerefactord.

- Bron: `C:\Users\user3\AndroidStudioProjects\EnigmaCast`
- Branch: `feature/torrents`, HEAD `40db96d3c47414112ff61138694b6405a2a4f445`
- Let op: de werkboom had bij het kopiëren al twee niet-gecommitte wijzigingen (`app/build.gradle.kts` en `.cursor/`); die zijn niet aangeraakt.
- Gekopieerd op: 2026-10-21
- SHA-256 per bestand, eerste 12 tekens; `identiek` betekent byte-voor-byte gelijk aan de bron.

## Samenvatting

| Status | Aantal |
|---|---:|
| gewijzigd | 7 |
| identiek | 131 |
| nieuw | 1 |
| **totaal** | **139** |

## Bestanden

| Module | Bestand in deze repo | Bron in EnigmaCast | Status | SHA-256 |
|---|---|---|---|---|
| core-model | `data/tmdb/TmdbJson.kt` | `app/src/main/java/com/enigmacast/data/tmdb/TmdbJson.kt` | identiek | `C633D87D2462` |
| core-model | `domain/account/AccountInput.kt` | `app/src/main/java/com/enigmacast/domain/account/AccountInput.kt` | identiek | `B39172589CE5` |
| core-model | `domain/account/AccountJson.kt` | `app/src/main/java/com/enigmacast/domain/account/AccountJson.kt` | identiek | `4AE1A7D2D2CE` |
| core-model | `domain/account/Entitlements.kt` | `app/src/main/java/com/enigmacast/domain/account/Entitlements.kt` | identiek | `393189583C49` |
| core-model | `domain/epg/GuideEvent.kt` | `app/src/main/java/com/enigmacast/domain/epg/GuideEvent.kt` | identiek | `33BE5EE1E76F` |
| core-model | `domain/epg/GuideTimeline.kt` | `app/src/main/java/com/enigmacast/domain/epg/GuideTimeline.kt` | identiek | `113EDE044E33` |
| core-model | `domain/epg/IptvEpgNames.kt` | `app/src/main/java/com/enigmacast/domain/epg/IptvEpgNames.kt` | identiek | `9F52ABF22AD4` |
| core-model | `domain/films/CountryResolver.kt` | `app/src/main/java/com/enigmacast/domain/films/CountryResolver.kt` | identiek | `919544F6D11C` |
| core-model | `domain/films/FilmDetector.kt` | `app/src/main/java/com/enigmacast/domain/films/FilmDetector.kt` | identiek | `716FBFF40313` |
| core-model | `domain/films/FilmMatcher.kt` | `app/src/main/java/com/enigmacast/domain/films/FilmMatcher.kt` | identiek | `440DE37E5D52` |
| core-model | `domain/films/FilmRanking.kt` | `app/src/main/java/com/enigmacast/domain/films/FilmRanking.kt` | identiek | `F8169BE2BBFA` |
| core-model | `domain/films/HomeFilmsBuilder.kt` | `app/src/main/java/com/enigmacast/domain/films/HomeFilmsBuilder.kt` | identiek | `A926A82DE47B` |
| core-model | `domain/films/TmdbApi.kt` | `app/src/main/java/com/enigmacast/domain/films/TmdbApi.kt` | identiek | `CEE67827FE3B` |
| core-model | `domain/model/ChannelMemory.kt` | `app/src/main/java/com/enigmacast/domain/model/ChannelMemory.kt` | identiek | `E4BF7B2CD15C` |
| core-model | `domain/model/Models.kt` | `app/src/main/java/com/enigmacast/domain/model/Models.kt` | gewijzigd | `DA4E022EF94F` |
| core-model | `domain/model/ReceiverLabel.kt` | `app/src/main/java/com/enigmacast/domain/model/ReceiverLabel.kt` | identiek | `BF3F6C78C125` |
| core-model | `domain/model/ReceiverList.kt` | `app/src/main/java/com/enigmacast/domain/model/ReceiverList.kt` | identiek | `12431CF0A666` |
| core-model | `domain/model/SkinDefaults.kt` | `app/src/main/java/com/enigmacast/domain/model/SkinDefaults.kt` | identiek | `5AD666019DD5` |
| core-model | `domain/network/QualitySession.kt` | `app/src/main/java/com/enigmacast/domain/network/QualitySession.kt` | identiek | `53C255A7B7EC` |
| core-model | `domain/network/ReceiverRouting.kt` | `app/src/main/java/com/enigmacast/domain/network/ReceiverRouting.kt` | identiek | `0443B51BFC58` |
| core-model | `domain/network/StreamQuality.kt` | `app/src/main/java/com/enigmacast/domain/network/StreamQuality.kt` | identiek | `DB451B41C881` |
| core-model | `domain/network/WireGuardText.kt` | `app/src/main/java/com/enigmacast/domain/network/WireGuardText.kt` | identiek | `9101E48E0F47` |
| core-model | `domain/oscam/HttpDigest.kt` | `app/src/main/java/com/enigmacast/domain/oscam/HttpDigest.kt` | identiek | `6E921271FCA4` |
| core-model | `domain/oscam/OscamStatus.kt` | `app/src/main/java/com/enigmacast/domain/oscam/OscamStatus.kt` | identiek | `1D3C43047585` |
| core-model | `domain/osd/OsdController.kt` | `app/src/main/java/com/enigmacast/domain/osd/OsdController.kt` | identiek | `6DB2D8826604` |
| core-model | `domain/osd/OsdEngine.kt` | `app/src/main/java/com/enigmacast/domain/osd/OsdEngine.kt` | identiek | `E04BE805E4BC` |
| core-model | `domain/osdinfo/CaSystems.kt` | `app/src/main/java/com/enigmacast/domain/osdinfo/CaSystems.kt` | identiek | `9A1FDA9769BC` |
| core-model | `domain/osdinfo/FrontendSignal.kt` | `app/src/main/java/com/enigmacast/domain/osdinfo/FrontendSignal.kt` | identiek | `1BC36D0435D6` |
| core-model | `domain/osdinfo/OsdInfo.kt` | `app/src/main/java/com/enigmacast/domain/osdinfo/OsdInfo.kt` | identiek | `C03FC22B2DFF` |
| core-model | `domain/osdinfo/OsdInfoBuilder.kt` | `app/src/main/java/com/enigmacast/domain/osdinfo/OsdInfoBuilder.kt` | identiek | `A8EC0A9B4666` |
| core-model | `domain/osdinfo/ReceiverFacts.kt` | `app/src/main/java/com/enigmacast/domain/osdinfo/ReceiverFacts.kt` | identiek | `836BD646FC7C` |
| core-model | `domain/osdinfo/ServiceOrigin.kt` | `app/src/main/java/com/enigmacast/domain/osdinfo/ServiceOrigin.kt` | identiek | `1998E0DC377A` |
| core-model | `domain/osdinfo/StreamFacts.kt` | `app/src/main/java/com/enigmacast/domain/osdinfo/StreamFacts.kt` | identiek | `0365A6E9AFA8` |
| core-model | `domain/player/PictureMode.kt` | `app/src/main/java/com/enigmacast/domain/player/PictureMode.kt` | identiek | `2140B1FC21DD` |
| core-model | `domain/player/QuickOptions.kt` | `app/src/main/java/com/enigmacast/domain/player/QuickOptions.kt` | identiek | `A5B746C3D2D5` |
| core-model | `domain/player/VodOsdKeys.kt` | `app/src/main/java/com/enigmacast/domain/player/VodOsdKeys.kt` | gewijzigd | `12A0020C7166` |
| core-model | `domain/power/PowerStateTracker.kt` | `app/src/main/java/com/enigmacast/domain/power/PowerStateTracker.kt` | identiek | `20B17A362C6A` |
| core-model | `domain/power/ReceiverPowerFlow.kt` | `app/src/main/java/com/enigmacast/domain/power/ReceiverPowerFlow.kt` | identiek | `355F6642C8F7` |
| core-model | `domain/profile/photo/HttpRequest.kt` | `app/src/main/java/com/enigmacast/domain/profile/photo/HttpRequest.kt` | identiek | `BAD33B5A9EE7` |
| core-model | `domain/profile/photo/PhotoImage.kt` | `app/src/main/java/com/enigmacast/domain/profile/photo/PhotoImage.kt` | identiek | `1D419A329E47` |
| core-model | `domain/profile/photo/PhotoPage.kt` | `app/src/main/java/com/enigmacast/domain/profile/photo/PhotoPage.kt` | identiek | `92D4F78A94E4` |
| core-model | `domain/profile/photo/QrCode.kt` | `app/src/main/java/com/enigmacast/domain/profile/photo/QrCode.kt` | identiek | `FED9008B4B8F` |
| core-model | `domain/profile/photo/UploadToken.kt` | `app/src/main/java/com/enigmacast/domain/profile/photo/UploadToken.kt` | identiek | `7D1D5DEECA06` |
| core-model | `domain/profile/ProfileCloud.kt` | `app/src/main/java/com/enigmacast/domain/profile/ProfileCloud.kt` | identiek | `A61A171E4D6E` |
| core-model | `domain/profile/Profiles.kt` | `app/src/main/java/com/enigmacast/domain/profile/Profiles.kt` | identiek | `91BCF58640BA` |
| core-model | `domain/recording/CaptureTarget.kt` | `app/src/main/java/com/enigmacast/domain/recording/CaptureTarget.kt` | identiek | `1095707F9AAE` |
| core-model | `domain/recording/MyItemsSections.kt` | `app/src/main/java/com/enigmacast/domain/recording/MyItemsSections.kt` | identiek | `B59EBAA980EC` |
| core-model | `domain/recording/Recording.kt` | `app/src/main/java/com/enigmacast/domain/recording/Recording.kt` | identiek | `5631E2F147FC` |
| core-model | `domain/reminder/Reminder.kt` | `app/src/main/java/com/enigmacast/domain/reminder/Reminder.kt` | identiek | `469DB905749D` |
| core-model | `domain/reminder/ReminderStorage.kt` | `app/src/main/java/com/enigmacast/domain/reminder/ReminderStorage.kt` | identiek | `88BC27D9DE93` |
| core-model | `domain/remote/EnigmaKey.kt` | `app/src/main/java/com/enigmacast/domain/remote/EnigmaKey.kt` | identiek | `F23A60185C3F` |
| core-model | `domain/remote/EnigmaKeyMapper.kt` | `app/src/main/java/com/enigmacast/domain/remote/EnigmaKeyMapper.kt` | identiek | `C97E98CC4843` |
| core-model | `domain/remote/KeyBindings.kt` | `app/src/main/java/com/enigmacast/domain/remote/KeyBindings.kt` | identiek | `D66D4414C971` |
| core-model | `domain/remote/ListAnchor.kt` | `app/src/main/java/com/enigmacast/domain/remote/ListAnchor.kt` | identiek | `2531F4D0E308` |
| core-model | `domain/resume/ResumePolicy.kt` | `app/src/main/java/com/enigmacast/domain/resume/ResumePolicy.kt` | identiek | `4C10A5569945` |
| core-model | `domain/resume/SeriesContinue.kt` | `app/src/main/java/com/enigmacast/domain/resume/SeriesContinue.kt` | identiek | `2E696FE89E95` |
| core-model | `domain/search/Search.kt` | `app/src/main/java/com/enigmacast/domain/search/Search.kt` | identiek | `7A2A04E85E12` |
| core-model | `domain/search/SearchQuery.kt` | `app/src/main/java/com/enigmacast/domain/search/SearchQuery.kt` | identiek | `25CAA7819A73` |
| core-model | `domain/security/PinLock.kt` | `app/src/main/java/com/enigmacast/domain/security/PinLock.kt` | identiek | `E655BB70D05C` |
| core-model | `domain/series/Series.kt` | `app/src/main/java/com/enigmacast/domain/series/Series.kt` | identiek | `5846C8432752` |
| core-model | `domain/service/BouquetChannels.kt` | `app/src/main/java/com/enigmacast/domain/service/BouquetChannels.kt` | identiek | `F2A6E962B1E9` |
| core-model | `domain/service/BouquetOrder.kt` | `app/src/main/java/com/enigmacast/domain/service/BouquetOrder.kt` | identiek | `9DBA0B0A4366` |
| core-model | `domain/service/LiveZapFlow.kt` | `app/src/main/java/com/enigmacast/domain/service/LiveZapFlow.kt` | identiek | `9BAA12DD7ACE` |
| core-model | `domain/service/ServiceChangeDetector.kt` | `app/src/main/java/com/enigmacast/domain/service/ServiceChangeDetector.kt` | identiek | `A2E2CEA22B10` |
| core-model | `domain/service/ServiceMonitor.kt` | `app/src/main/java/com/enigmacast/domain/service/ServiceMonitor.kt` | identiek | `F552EED187EC` |
| core-model | `domain/service/ServiceStreams.kt` | `app/src/main/java/com/enigmacast/domain/service/ServiceStreams.kt` | identiek | `ACF9FD36A33C` |
| core-model | `domain/service/StreamReuse.kt` | `app/src/main/java/com/enigmacast/domain/service/StreamReuse.kt` | identiek | `34F79247696B` |
| core-model | `domain/settingsimport/ImportPlan.kt` | `app/src/main/java/com/enigmacast/domain/settingsimport/ImportPlan.kt` | identiek | `D025CF3576AE` |
| core-model | `domain/settingsimport/ImportProgress.kt` | `app/src/main/java/com/enigmacast/domain/settingsimport/ImportProgress.kt` | identiek | `B43A7C01EDA6` |
| core-model | `domain/settingsimport/ImportSchedule.kt` | `app/src/main/java/com/enigmacast/domain/settingsimport/ImportSchedule.kt` | identiek | `F24DEF532ACB` |
| core-model | `domain/settingsimport/M3u.kt` | `app/src/main/java/com/enigmacast/domain/settingsimport/M3u.kt` | identiek | `534E2F140DD0` |
| core-model | `domain/settingsimport/ReloadCheck.kt` | `app/src/main/java/com/enigmacast/domain/settingsimport/ReloadCheck.kt` | identiek | `4C05B1DA20EB` |
| core-model | `domain/settingsimport/SettingsRepos.kt` | `app/src/main/java/com/enigmacast/domain/settingsimport/SettingsRepos.kt` | identiek | `6E2098A13B76` |
| core-model | `domain/settingsimport/TransferErrors.kt` | `app/src/main/java/com/enigmacast/domain/settingsimport/TransferErrors.kt` | identiek | `9C58B26BB559` |
| core-model | `domain/setup/SetupStep.kt` | `app/src/main/java/com/enigmacast/domain/setup/SetupStep.kt` | identiek | `744FADDEB8ED` |
| core-model | `domain/soccer/MatchChannelFinder.kt` | `app/src/main/java/com/enigmacast/domain/soccer/MatchChannelFinder.kt` | identiek | `D5F174F40C17` |
| core-model | `domain/soccer/pitch/PitchJson.kt` | `app/src/main/java/com/enigmacast/domain/soccer/pitch/PitchJson.kt` | identiek | `11265FED076D` |
| core-model | `domain/soccer/pitch/PitchModels.kt` | `app/src/main/java/com/enigmacast/domain/soccer/pitch/PitchModels.kt` | identiek | `41C859EFCFC5` |
| core-model | `domain/soccer/pitch/PitchStats.kt` | `app/src/main/java/com/enigmacast/domain/soccer/pitch/PitchStats.kt` | identiek | `B1AA3C0914C3` |
| core-model | `domain/soccer/RegionLoads.kt` | `app/src/main/java/com/enigmacast/domain/soccer/RegionLoads.kt` | identiek | `90BEAF3766BF` |
| core-model | `domain/soccer/RequestWindow.kt` | `app/src/main/java/com/enigmacast/domain/soccer/RequestWindow.kt` | identiek | `35DE50FE4E45` |
| core-model | `domain/soccer/SoccerCountries.kt` | `app/src/main/java/com/enigmacast/domain/soccer/SoccerCountries.kt` | identiek | `B46ACCC4499B` |
| core-model | `domain/soccer/SoccerHome.kt` | `app/src/main/java/com/enigmacast/domain/soccer/SoccerHome.kt` | identiek | `AD9F809CBC65` |
| core-model | `domain/soccer/SoccerJson.kt` | `app/src/main/java/com/enigmacast/domain/soccer/SoccerJson.kt` | identiek | `A257CAC0BE36` |
| core-model | `domain/soccer/SoccerModels.kt` | `app/src/main/java/com/enigmacast/domain/soccer/SoccerModels.kt` | identiek | `86576A0EB5F3` |
| core-model | `domain/soccer/SportRegions.kt` | `app/src/main/java/com/enigmacast/domain/soccer/SportRegions.kt` | identiek | `A3F7D74A46AF` |
| core-model | `domain/soccer/StandingZones.kt` | `app/src/main/java/com/enigmacast/domain/soccer/StandingZones.kt` | identiek | `9EBB3E57DFE3` |
| core-model | `domain/source/Sources.kt` | `app/src/main/java/com/enigmacast/domain/source/Sources.kt` | identiek | `DBF4F6DE80B7` |
| core-model | `domain/subsource/SubSource.kt` | `app/src/main/java/com/enigmacast/domain/subsource/SubSource.kt` | identiek | `4C7013151EE0` |
| core-model | `domain/sync/CacheWorkOrder.kt` | `app/src/main/java/com/enigmacast/domain/sync/CacheWorkOrder.kt` | identiek | `92699FB9A3E1` |
| core-model | `domain/sync/ChannelLists.kt` | `app/src/main/java/com/enigmacast/domain/sync/ChannelLists.kt` | identiek | `B4B4825E671B` |
| core-model | `domain/sync/EpgSyncDecision.kt` | `app/src/main/java/com/enigmacast/domain/sync/EpgSyncDecision.kt` | identiek | `07E90DBDC927` |
| core-model | `domain/sync/GuideCacheReader.kt` | `app/src/main/java/com/enigmacast/domain/sync/GuideCacheReader.kt` | identiek | `E0BA0A4DD514` |
| core-model | `domain/sync/HomeBouquets.kt` | `app/src/main/java/com/enigmacast/domain/sync/HomeBouquets.kt` | identiek | `CE798888EDCB` |
| core-model | `domain/sync/LibraryMerge.kt` | `app/src/main/java/com/enigmacast/domain/sync/LibraryMerge.kt` | identiek | `1DC747C8CDCD` |
| core-model | `domain/sync/LibrarySync.kt` | `app/src/main/java/com/enigmacast/domain/sync/LibrarySync.kt` | identiek | `7F3117E2E05D` |
| core-model | `domain/sync/StartupChannelCheck.kt` | `app/src/main/java/com/enigmacast/domain/sync/StartupChannelCheck.kt` | identiek | `ECA4ED8BA811` |
| core-model | `domain/sync/SyncConflict.kt` | `app/src/main/java/com/enigmacast/domain/sync/SyncConflict.kt` | identiek | `26D5B7AC1CC5` |
| core-model | `domain/sync/SyncPolicy.kt` | `app/src/main/java/com/enigmacast/domain/sync/SyncPolicy.kt` | identiek | `03DB626BC57A` |
| core-model | `domain/torrent/LastStream.kt` | `app/src/main/java/com/enigmacast/domain/torrent/LastStream.kt` | identiek | `E0FC310D37F4` |
| core-model | `domain/torrent/StreamWindow.kt` | `app/src/main/java/com/enigmacast/domain/torrent/StreamWindow.kt` | identiek | `D8F5B5BE5261` |
| core-model | `domain/torrent/Stremio.kt` | `app/src/main/java/com/enigmacast/domain/torrent/Stremio.kt` | gewijzigd | `DDF60D963E82` |
| core-model | `domain/torrent/TorrentLibraryRows.kt` | `app/src/main/java/com/enigmacast/domain/torrent/TorrentLibraryRows.kt` | identiek | `558A088EED13` |
| core-model | `domain/torrent/TorrentTmdb.kt` | `app/src/main/java/com/enigmacast/domain/torrent/TorrentTmdb.kt` | identiek | `3EE1D1F0D4D0` |
| core-model | `domain/tuner/ChannelNavigator.kt` | `app/src/main/java/com/enigmacast/domain/tuner/ChannelNavigator.kt` | identiek | `0CDA11947E21` |
| core-model | `domain/tuner/Lamedb.kt` | `app/src/main/java/com/enigmacast/domain/tuner/Lamedb.kt` | identiek | `BE5339E1C1BD` |
| core-model | `domain/tuner/NowEvents.kt` | `app/src/main/java/com/enigmacast/domain/tuner/NowEvents.kt` | identiek | `A59C3A99BCE6` |
| core-model | `domain/tuner/OwnTunerKeys.kt` | `app/src/main/java/com/enigmacast/domain/tuner/OwnTunerKeys.kt` | identiek | `60620AB0C1F3` |
| core-model | `domain/tuner/RecordingGuard.kt` | `app/src/main/java/com/enigmacast/domain/tuner/RecordingGuard.kt` | identiek | `F4B0D92662BC` |
| core-model | `domain/tuner/RestartBudget.kt` | `app/src/main/java/com/enigmacast/domain/tuner/RestartBudget.kt` | identiek | `32D38AE2DCA8` |
| core-model | `domain/tuner/ServiceRef.kt` | `app/src/main/java/com/enigmacast/domain/tuner/ServiceRef.kt` | identiek | `59499DDA1A8F` |
| core-model | `domain/tuner/SignalReferences.kt` | `app/src/main/java/com/enigmacast/domain/tuner/SignalReferences.kt` | identiek | `A79E8EEF6334` |
| core-model | `domain/tuner/StreamModePolicy.kt` | `app/src/main/java/com/enigmacast/domain/tuner/StreamModePolicy.kt` | identiek | `04480BFB2337` |
| core-model | `domain/tuner/StreamStartCheck.kt` | `app/src/main/java/com/enigmacast/domain/tuner/StreamStartCheck.kt` | identiek | `31BF7F73A10F` |
| core-model | `domain/tuner/TunerAvailability.kt` | `app/src/main/java/com/enigmacast/domain/tuner/TunerAvailability.kt` | identiek | `D8EA812EE609` |
| core-model | `domain/tuner/TunerBoard.kt` | `app/src/main/java/com/enigmacast/domain/tuner/TunerBoard.kt` | identiek | `C804C321DAD2` |
| core-model | `domain/tuner/TunerSatellites.kt` | `app/src/main/java/com/enigmacast/domain/tuner/TunerSatellites.kt` | identiek | `2478743F8A66` |
| core-model | `domain/ui/FocusSettle.kt` | `app/src/main/java/com/enigmacast/domain/ui/FocusSettle.kt` | identiek | `997B91CC2B66` |
| core-model | `domain/validation/ReceiverConfigValidator.kt` | `app/src/main/java/com/enigmacast/domain/validation/ReceiverConfigValidator.kt` | gewijzigd | `904122E79576` |
| core-model | `domain/xtream/Catchup.kt` | `app/src/main/java/com/enigmacast/domain/xtream/Catchup.kt` | identiek | `9647E6AE2AAD` |
| core-model | `domain/xtream/ChannelMatch.kt` | `app/src/main/java/com/enigmacast/domain/xtream/ChannelMatch.kt` | identiek | `26D5BF5C6182` |
| core-model | `domain/xtream/IptvCategoryFilter.kt` | `app/src/main/java/com/enigmacast/domain/xtream/IptvCategoryFilter.kt` | identiek | `876793EC62E9` |
| core-model | `domain/xtream/IptvLibraryRows.kt` | `app/src/main/java/com/enigmacast/domain/xtream/IptvLibraryRows.kt` | identiek | `91D362037831` |
| core-model | `domain/xtream/IptvMini.kt` | `app/src/main/java/com/enigmacast/domain/xtream/IptvMini.kt` | identiek | `A361F8487B1C` |
| core-model | `domain/xtream/IptvPlayerKeys.kt` | `app/src/main/java/com/enigmacast/domain/xtream/IptvPlayerKeys.kt` | gewijzigd | `45705C871F9F` |
| core-model | `domain/xtream/IptvSearch.kt` | `app/src/main/java/com/enigmacast/domain/xtream/IptvSearch.kt` | identiek | `A13F73F8AD70` |
| core-model | `domain/xtream/LazyKeys.kt` | `app/src/main/java/com/enigmacast/domain/xtream/LazyKeys.kt` | identiek | `55B7ED624521` |
| core-model | `domain/xtream/LibraryCardAction.kt` | `app/src/main/java/com/enigmacast/domain/xtream/LibraryCardAction.kt` | identiek | `43686C4D540A` |
| core-model | `domain/xtream/LibraryReturn.kt` | `app/src/main/java/com/enigmacast/domain/xtream/LibraryReturn.kt` | identiek | `C3F93CD07EB3` |
| core-model | `domain/xtream/M3uPlaylist.kt` | `app/src/main/java/com/enigmacast/domain/xtream/M3uPlaylist.kt` | identiek | `62A8BDBC93E6` |
| core-model | `domain/xtream/NextEpisode.kt` | `app/src/main/java/com/enigmacast/domain/xtream/NextEpisode.kt` | identiek | `95AECB860796` |
| core-model | `domain/xtream/RowGuide.kt` | `app/src/main/java/com/enigmacast/domain/xtream/RowGuide.kt` | identiek | `849C659670E4` |
| core-model | `domain/xtream/SeriesPlay.kt` | `app/src/main/java/com/enigmacast/domain/xtream/SeriesPlay.kt` | identiek | `6EB5B6320A02` |
| core-model | `domain/xtream/Xtream.kt` | `app/src/main/java/com/enigmacast/domain/xtream/Xtream.kt` | identiek | `BEC95D4A2ED1` |
| core-openwebif | `data/openwebif/AutoTimerXml.kt` | `app/src/main/java/com/enigmacast/data/openwebif/AutoTimerXml.kt` | identiek | `FF1C9B59D93A` |
| core-openwebif | `data/openwebif/OpenWebifEndpoints.kt` | `app/src/main/java/com/enigmacast/data/openwebif/OpenWebifEndpoints.kt` | gewijzigd | `2B0208652696` |
| core-openwebif | `data/openwebif/OpenWebifException.kt` | `app/src/main/java/com/enigmacast/data/openwebif/OpenWebifException.kt` | nieuw | `9DE7532BA506` |
| core-openwebif | `data/openwebif/OpenWebifJson.kt` | `app/src/main/java/com/enigmacast/data/openwebif/OpenWebifJson.kt` | gewijzigd | `ED780BA20CEC` |
| core-openwebif | `data/openwebif/OsdInfoJson.kt` | `app/src/main/java/com/enigmacast/data/openwebif/OsdInfoJson.kt` | identiek | `6C787BCB868F` |

## Bewust niet gekopieerd

| Onderdeel | Reden |
|---|---|
| `ui/**` (Compose for TV) | `androidx.tv.material3`, D-pad-focus en 16:9-aannames; de Companion krijgt een eigen Material 3-UI. |
| `MainActivity`, `AppViewModel` | Activity- en TV-lifecycle; de brug in fase 2 levert alleen smalle hooks. |
| `LivePlaybackController` e.a. | ExoPlayer + SurfaceView + TV-output; alleen als referentie gebruikt. |
| `data/openwebif/OpenWebifApi.kt` | Verweven met `ReceiverRouter`, `OkHttpClientFactory` en FTP; de Companion bouwt in fase 5 een dunne client op de wél gedeelde parsers. |
| `data/db/EnigmaDatabase`, `UserDatabase` (DAO's) | `Context`, Hilt en TV-specifieke tabellen; alleen de entiteiten zijn gedeeld. |
| `PhotoUploadServer` | Ongeauthenticeerde eenmalige server; vervangen door een geauthenticeerd brug-endpoint. |
| `data/torrent`, `torrent/**` | Buiten de Companion-scope; het domein blijft staan omdat `resume` er één type van gebruikt. |

## Aangebrachte shims

| Bestand | Wijziging |
|---|---|
| `domain/model/Models.kt` | `OsdSkinId.label` (`@StringRes Int`) → `labelKey: String`; `R`- en `StringRes`-imports weg. |
| `domain/validation/ReceiverConfigValidator.kt` | `R.string.*` → `CoreStrings.*`. |
| `domain/player/VodOsdKeys.kt` | `android.view.KeyEvent` → `CoreKey`. |
| `domain/xtream/IptvPlayerKeys.kt` | `android.view.KeyEvent` → `CoreKey`. |
| `domain/torrent/Stremio.kt` | `httpUrl()` (OkHttp) verwijderd; de Companion speelt geen torrents. |
| `data/openwebif/OpenWebifEndpoints.kt` | `okhttp3.Credentials.basic` → `java.util.Base64`. |
| `data/openwebif/OpenWebifJson.kt` | `R.string.*` → `CoreStrings.*`; `OpenWebifJson` en `GrabSupport` publiek. |
| `data/openwebif/OpenWebifException.kt` | Nieuw: de exception die in `OpenWebifApi.kt` stond. |
| `util/UiText.kt`, `util/EnigmaLog.kt` | JVM-varianten zonder `Context`/Timber; de app levert de resolvers. |
| `util/CoreKey.kt`, `util/CoreStrings.kt` | Nieuwe constanten voor toetscodes en string-id's. |
| `data/db/LibraryRows.kt` | De drie gebruikersentiteiten uit `UserDatabase.kt`, zonder DAO's. |
| `data/xtream/IptvPlaylistLogin.kt` | Het loginmodel uit `IptvPlaylist.kt`, zonder de opslag. |
| `data/tmdb/TmdbJson.kt` | Ongewijzigd gekopieerd; staat in `core-model` omdat films/series het nodig hebben. |


## Nieuwe code (niet gekopieerd)

Deze bestanden zijn nieuw geschreven voor de Companion en hebben geen bron in EnigmaCast:

- `mobile/app/**` — de Android-app (Compose, ViewModel, MockBridge, thema).
- `shared/core-protocol/**` — het protocol v1 en de contracttests.
- `tools/tls-spike/**` — het JVM-bewijs voor certificaat en SPKI-pinning.
- `protocol/**` — de gouden JSON-voorbeelden.
- `docs/**` — protocol, dreigingsmodel, TLS-spike en ADR's.

## Wijzigingen in de EnigmaCast TV-repo (fase 2)

De TV-brug is nieuwe code in EnigmaCast; de bestaande architectuur is niet gerefactord.

| Bestand | Wijziging |
|---|---|
| `app/src/main/java/com/enigmacast/companionbridge/**` | Nieuw: TLS/HTTP/WS-server, auth, koppeling, status, commando's, UI-paneel |
| `app/src/test/java/com/enigmacast/companionbridge/CompanionBridgeTest.kt` | Nieuw: 5 unittests (WS-accept, pairing, envelope, handtekening) |
| `app/build.gradle.kts` | `buildConfigField("boolean", "COMPANION_BRIDGE", "true")` |
| `MainActivity.kt` | Injectie, hooks, onForeground/onBackground/shutdown |
| `ui/dashboard/DashboardScreen.kt` | Settings-pagina + menu-regel |
| `res/values/strings.xml`, `res/values-nl/strings.xml` | 22 nieuwe strings |
| `README.md` | Sectie Phone remote (Companion bridge) |

Uitkomst TV-suite: **189 testklassen, 928 tests, 0 failures**. End-to-end rooktest:
zie `docs/tv-bridge.md`. De Companion-app gebruikt nog de mock.

## Fase 2 — echte telefoon-client

De Companion-app spreekt nu de echte TV-brug; de mock blijft voor de demo.

| Bestand | Wijziging |
|---|---|
| `data/net/DeviceIdentity.kt` | ECDSA P-256 in de Android Keystore |
| `data/net/PinnedTls.kt` | SPKI-pinning / TOFU |
| `data/net/PairingClient.kt` | `POST /api/v1/pair` |
| `data/net/WebSocketConnection.kt` | Getekende WSS-upgrade, hello/state/ack, reconnect |
| `data/net/NsdDiscovery.kt` + `NsdTxt.kt` | `_enigmacast._tcp.` en TXT-parsing |
| `data/net/Backoff.kt` | Reconnect-backoff met jitter |
| `data/SecureStore.kt` | Token AES-GCM versleuteld |
| `data/Models.kt` / `LocalStore.kt` | Pin, token, deviceId, `real` |
| `AppContainer.kt` / `ui/AppViewModel.kt` | Echte vs. demo-verbinding, scan/pair/forget |
| `ui/screens/OnboardingScreens.kt` | NSD-resultaten, handmatig adres, pair-status |
| `ui/screens/MoreScreens.kt` | Echte brug, pin en diagnose |
| `AndroidManifest.xml` / `build.gradle.kts` | Multicast-permissie, OkHttp |

Uitkomst: **125 klassen, 666 tests, 0 failures** (1 optionele rooktest overgeslagen).
End-to-end bewezen op twee emulators; zie `docs/tv-bridge.md`.

## Fase 3 — formulieren en tekstinvoer

De TV-brug en de Companion kunnen nu tekst uitwisselen voor elk TvTextField.

| Bestand | Wijziging |
|---|---|
| `app/src/main/java/com/enigmacast/companionbridge/CompanionFormHost.kt` | Nieuw: sessie, live sync, submit/cancel, main-thread-callback |
| `app/src/main/java/com/enigmacast/companionbridge/CompanionFormHub.kt` | Nieuw: haak voor de UI |
| `app/src/main/java/com/enigmacast/ui/theme/TvTextField.kt` | Opent/sluit de sessie bij bewerken |
| `CompanionProtocol.kt` / `CompanionServer.kt` / `CompanionBridge.kt` | `form`/`cmd.form`, broadcast, capabilities |
| `CompanionLinkPanel.kt` / `CompanionViewModel.kt` / strings | Testveld in Settings › Phone |
| `shared/core-protocol/.../Messages.kt` | `FormDescriptor.closed`, `FormSubmitCommand.action` |
| `mobile/.../ui/components/FormSheet.kt` | Nieuw: toetsenbord-sheet |
| `mobile/.../ui/AppViewModel.kt` / `CompanionApp.kt` | Form-state, TEXT/SUBMIT/CANCEL |

Uitkomst: **TV 189 klassen, 929 tests, 0 failures**; **Companion 125 klassen, 667 tests,
0 failures, 1 optionele rooktest overgeslagen**. End-to-end: TV-zoekveld live gevuld vanaf
de telefoon; zie `docs/tv-bridge.md` en `docs/adr/0009-formulieren-via-tvtextfield.md`.

## Fase 4 — TV-instellingen en profielfoto's

| Bestand | Wijziging |
|---|---|
| `app/src/main/java/com/enigmacast/companionbridge/CompanionSettingsHost.kt` | Nieuw: catalogus (zes paden), revision, cmd.settings |
| `CompanionServer.kt` | `state.settings`, `PUT /api/v1/profiles/{id}/photo`, 10 MB-limiet, `profile`-wrapper |
| `CompanionBridge.kt` | settings/profielen-collectors, `settingsJson`, `savePhoto`, `profilesJson` |
| `CompanionProtocol.kt` | `state.settings`/`cmd.settings`, `MAX_PHOTO_BYTES`, `CompanionProfile` |
| `res/values*/strings.xml` | labels voor de catalogus |
| `mobile/.../ui/screens/MoreScreens.kt` | TV-instellingen (BOOL/CHOICE/NUMBER), foto's kiezen/testen |
| `mobile/.../ui/AppViewModel.kt` | catalogus, `cmd.settings`, fotoupload, foutbestendige events |
| `mobile/.../data/net/PhotoUploadClient.kt` | Nieuw: getekende PUT |
| `mobile/.../AppContainer.kt` | fotoclient |
| `shared/core-protocol/.../Messages.kt` + golden `state-settings.json` | catalogus-DTO's en contracttest |

Uitkomst: **TV 189 klassen, 929 tests, 0 failures**; **Companion 125 klassen, 667 tests,
0 failures, 1 optionele rooktest overgeslagen**. End-to-end: instelling aan/uit en de
"EC"-testfoto op het Enrico-profiel. Zie `docs/tv-bridge.md` en
`docs/adr/0010-instellingen-en-fotos.md`.

## Fase 4.1 — volledige catalogus en profielbeheer

| Bestand | Wijziging |
|---|---|
| `CompanionSettingsHost.kt` | 24 instellingen in vijf categorieën, `category`/`dangerous`/`requiresRestart` |
| `CompanionBridge.kt` | `CREATE`/`RENAME`/`SET_AVATAR`/`DELETE`, profiel-PIN, `ProfileCleanup`, foto-PIN-blokkade |
| `CompanionProtocol.kt` | `CompanionProfile.hasPin`, `ERR_VALIDATION` |
| `CompanionSettingsHost` / strings | nieuwe labels en opties |
| `mobile/.../ui/screens/MoreScreens.kt` | gegroepeerde catalogus, gevarenbevestiging, profieleditor, PIN-dialoog, verwijderen, foto wissen |
| `mobile/.../ui/AppViewModel.kt` | profiel-CRUD, foutcodemapping |
| `shared/core-protocol/Messages.kt` + golden | `SettingEntry`-metadata, `ProfileCommand.clearPhoto` |

Uitkomst: **TV 189 klassen, 929 tests, 0 failures**; **Companion 125 klassen, 667 tests,
0 failures, 1 optionele rooktest overgeslagen**. End-to-end: profiel aanmaken → hernoemen
→ avatar → verwijderen, foto wissen, gevarenbevestiging. Zie `docs/tv-bridge.md` en
`docs/adr/0011-volledige-catalogus-profielbeheer.md`.

## Fase 5–7 — live, IPTV en bibliotheek

| Bestand | Wijziging |
|---|---|
| `data/live/EnigmaLiveClient.kt` | OpenWebif-lijsten/EPG en stream-URL's, optionele Basic auth, HTTPS |
| `data/live/IptvClient.kt` | M3U laden en headers doorgeven |
| `data/live/XtreamClient.kt` | Xtream-login, live-categorieën en -streams |
| `data/library/LibraryStore.kt` | lokale geschiedenis, favorieten en voortgang |
| `ui/screens/LiveScreen.kt` / `IptvScreen.kt` / `XtreamScreen.kt` / `LibraryScreen.kt` | fase 5–7-schermen |
| `ui/screens/PlayerScreen.kt` | Media3 ExoPlayer, `PlayerSurface`, ster en voortgang |
| `AppViewModel.kt` / `AppScreen.kt` / `CompanionApp.kt` | live/IPTV/bibliotheekstate en routes |
| `LocalStore.kt` | ontvangerconfig (wachtwoord in de Keystore), M3U-URL, Xtream-login |
| `gradle/libs.versions.toml` / `mobile/app/build.gradle.kts` | Media3 1.11.1 en `core-openwebif` |
| `tools/fake-live.py` | fake OpenWebif, WAV-stream, M3U en Xtream voor de emulator |

Uitkomst: **Companion 125 klassen, 667 tests, 0 failures, 1 optionele rooktest
overgeslagen**; **TV 189 klassen, 929 tests, 0 failures**. End-to-end: Live met
zenderlijst/EPG en "Speelt", M3U-groepen en stream, Xtream-login/categorie/stream,
"Verder kijken" en favoriet. Zie `docs/playback.md`, ADR 0012/0013.

## Fase 6.1 + 4.1 — VOD/series en Settings-PIN in de telefoon

| Bestand | Wijziging |
|---|---|
| `data/live/XtreamClient.kt` | Films/series: `get_vod_*`, `get_series*`, afleveringen, film-/episode-URL's |
| `ui/screens/XtreamScreen.kt` | secties Live/Films/Series, afleveringenlijst |
| `ui/AppViewModel.kt` | VOD/series-state, `watchXtreamEpisode`, terugkeer naar de bron na de speler |
| `shared/core-protocol/Messages.kt` | `SettingsCommand.pin`, `SettingsCatalogPayload.locked` |
| TV `CompanionSettingsHost.kt` | volledige catalogus zichtbaar bij PIN, `unlock(pin)`, server-side weigering |
| `ui/screens/MoreScreens.kt` | PIN-banner en PIN-dialoog voor gevorderde instellingen |
| `tools/fake-live.py` | VOD/series in de fake Xtream |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
End-to-end: Xtream Films → Testfilm → "Speelt"; Series → Testserie → S1E1 → "Speelt";
terug naar de afleveringen. De Settings-PIN is gecompileerd en via de unit-suite
gecontroleerd; een volledige TV-PIN-E2E is op de emulator niet gelukt (TV-navigatie).

## Fase 7.1 + 4.2 — gedeelde IPTV-bibliotheek en foto-GET/DELETE

| Bestand | Wijziging |
|---|---|
| `shared/core-protocol/Protocol.kt` | `state.library`, `cmd.library`, capability `library` |
| `shared/core-protocol/Messages.kt` | `LibraryEntry`, `LibraryStatePayload`, `LibraryCommand`, `LibraryAction` |
| TV `CompanionLibraryHost.kt` | favorieten/posities van het actieve profiel, `setFavorite`/`saveResume` |
| TV `CompanionBridge.kt` / `CompanionServer.kt` | `state.library` bij connect/broadcast, `cmd.library`, foto-`GET`/`DELETE` |
| `mobile/.../data/library/LibraryStore.kt` | `tvSource/kind/id`, `mergeTv`, SecureStore |
| `mobile/.../ui/AppViewModel.kt` | library-state, refs, `PROGRESS`/`FAVORITE` terugschrijven |
| `mobile/.../data/net/PhotoUploadClient.kt` | `fetch`/`delete` (getekend) |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
End-to-end: een op de telefoon gespeelde Xtream-film (Testfilm) verscheen op de TV als
`iptv_resume` (18 s/60 s) en `iptv_library` voor het actieve profiel; de TV-bibliotheek
wordt bij verbinden naar de telefoon gestuurd. Foto-`GET`/`DELETE` zijn als API
geïmplementeerd en gecompileerd; de UI gebruikt nog de profielcommando's.

## Fase 7.2 + 4.3 — foto-overzicht en catch-up

| Bestand | Wijziging |
|---|---|
| `ui/AppViewModel.kt` | `loadProfilePhoto`/`clearPhotoPreview`, `watchXtreamCatchup` |
| `ui/screens/MoreScreens.kt` | "Bekijk foto" en fotodialoog op de getekende `GET` |
| `ui/screens/XtreamScreen.kt` | "Terugkijken (laatste uur)" bij archiefzenders |
| `tools/fake-live.py` | `tv_archive` op NPO 1 en `/timeshift/`-stream |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
End-to-end: de EC-foto van Enrico werd met `GET` op de telefoon getoond; NPO 1 uit de fake
Xtream speelde via `/timeshift/...` ("NPO 1 · terugkijken", "Speelt").

## Fase 7.3 — catch-up met EPG

| Bestand | Wijziging |
|---|---|
| `data/live/XtreamClient.kt` | `epg()` via `get_short_epg` en `XtreamJson.shortEpg` |
| `ui/AppViewModel.kt` | `openCatchup`, `playCatchup`, archief-filter met `Catchup.canPlay` |
| `ui/screens/XtreamScreen.kt` | programma-dialoog met tijden en "Sluiten" |
| `tools/fake-live.py` | `get_short_epg` met base64-titels |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
End-to-end: NPO 1 → Terugkijken → NOS Journaal/Nieuwsuur/Documentaire → NOS Journaal →
"NPO 1 · NOS Journaal", "Speelt".

## Fase 7.4 — recente zenders van de TV

| Bestand | Wijziging |
|---|---|
| TV `CompanionBridge.kt` | `recentJson` uit `ChannelMemory.recentFor` van de actieve ontvanger |
| TV `CompanionServer.kt` | `state.recent` bij connect/broadcast |
| `shared/core-protocol` | `RecentChannel`, `RecentStatePayload`, `state.recent`, capability `history` |
| `ui/AppViewModel.kt` / `LiveScreen.kt` | `recentTv`, "Recent op de tv", `watchRecentChannel` |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
End-to-end: NPO1 HD/NPO2 HD/NPO3 HD van de TV verschenen op de telefoon; NPO1 HD speelde
("Speelt"). De referenties zijn alleen geldig op dezelfde ontvanger.

## Fase 7.5 — telefoon → TV live-historie

| Bestand | Wijziging |
|---|---|
| `shared/core-protocol` | `RecentCommand`, `cmd.recent`, `receiverHost` in `RecentStatePayload` |
| TV `CompanionBridge.kt` | `rememberRecent` met hostcontrole en `ChannelMemory.remember` |
| `ui/AppViewModel.kt` | `recentTvHost`, `cmd.recent` na een live-start op dezelfde host |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
De hostcontrole voorkomt dat een telefoon met een andere ontvanger de TV-historie wijzigt.
Een volledige E2E met een gedeelde host is op de emulators niet gedaan (TV en telefoon
gebruiken verschillende receivers); de TV→telefoon-route is wel end-to-end bewezen.

## Fase 8 — meerveldsformulieren

| Bestand | Wijziging |
|---|---|
| TV `CompanionFormHost.kt` | `openForm` met meerdere `FieldSpec`s, `update(id, fieldId, text)`, `submit(id, values)` |
| TV `CompanionBridge.kt` | `formJson` stuurt alle velden; `cmd.form` verwerkt de waardemap |
| TV `SetupReceiverStep.kt` | **"Vul via telefoon"** voor host/gebruikersnaam/wachtwoord |
| TV `strings.xml` | "Fill via phone"/"Vul via telefoon" en de formuliertitel |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
De host, DTO's en telefoon renderen meerdere velden; de ontvanger-setupknop opent ze in één
sessie. Een volledige E2E op de setup-pagina was op de emulator niet mogelijk (de pagina
was niet bereikbaar vanuit de huidige TV-staat).

## Fase 7.6 + 7.7 — IPTV-bron sync en fullscreen speler

| Bestand | Wijziging |
|---|---|
| TV `CompanionBridge.kt` | `state.iptv` uit `IptvPlaylistStore` en `EnigmaSyncEngine.peekPlaylist()`, bij start en client-connect |
| TV `CompanionServer.kt` / `CompanionProtocol.kt` | `state.iptv` (seq 6), capability `iptv` |
| `shared/core-protocol` | `IptvSourcePayload`, `state.iptv` |
| `ui/AppViewModel.kt` | automatische M3U/Xtream-overname, `iptvFromTv`, auto-login bij openen |
| `data/LocalStore.kt` | M3U-URL in `SecureStore` |
| `ui/screens/PlayerScreen.kt` | landscape, immersive fullscreen, cutout, `VideoSize`-aspect, terugknop, decoder-fallback |
| `ui/CompanionApp.kt` | player zonder veilige-area-inset |

Uitkomst: **Companion 667 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
Bewezen: live-speler draait op 2992×1344 (ROTATION_90) zonder systeembalken en met correcte
terugkeer naar portrait. De IPTV-bron is gecompileerd; een echte M3U/Xtream-sync is niet
end-to-end getest omdat de TV-playliststore in de emulator leeg is. De MP4-test op de
emulator faalt op de H.264-decoder, niet op de app.

## Release 1.0.0-beta.2

- `versionCode` 101, `versionName` 1.0.0-beta.2.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.2-build101-sideload.apk`.
- Update-JSON: companion 101 in het `companion`-blok van `enigmacast-v2.json`.
- De release-APK is met de debug-key ondertekend (sideload).

## Release 1.0.0-beta.3

- In-app updatecheck via `enigmacast-v2.json` (`companion`-blok) met terugval op
  `enigmacast-companion.json`; auto bij start, handmatig via **Meer › Controleer op updates**.
  Download naar cache, FileProvider en `REQUEST_INSTALL_PACKAGES` voor de installatie.
- Launcher-icoon: dezelfde adaptieve TV-iconen (`ic_launcher_foreground` + `#080D1C`).
- `versionCode` 102, `versionName` 1.0.0-beta.3.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.3-build102-sideload.apk`.
- **Companion 670 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.

## Release 1.0.0-beta.4

- NSD: de TV adverteert `_enigmacast._tcp.` via `CompanionNsd` (id/naam/app/pin in TXT);
  de telefoon vraagt op Android 16+ `ACCESS_LOCAL_NETWORK` en resolve't vóór het stoppen
  van de discovery.
- `versionCode` 103, `versionName` 1.0.0-beta.4.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.4-build103-sideload.apk`.
- **Companion 670 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.
- Bewezen: TV logt `CompanionNsd: geregistreerd: AOSP TV on x86`; telefoon toont de
  lokale-netwerkpermission en `ACCESS_LOCAL_NETWORK: granted=true`. Een echte
  TV-telefoon-NSD-match op hetzelfde LAN is nog niet op hardware getest.

## Release 1.0.0-beta.5

- IPTV gaat direct naar de inhoud als er een M3U/Xtream-bron is; de setup verschijnt alleen
  via **Wijzigen**.
- Zenderlogo's via Coil, bronregel, categoriechips en een nettere Live/Films/Series-lijst.
- Fullscreen-speler: linker zenderlijst met logo's (ongeveer 6–8 zenders in landscape),
  direct zappen, verbergbaar; onderbalk schuift naar rechts.
- Films/series: TMDB-infodialoog met poster, jaar, score en beschrijving; titels worden
  genormaliseerd en alleen exacte matches worden getoond.
- `versionCode` 104, `versionName` 1.0.0-beta.5.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.5-build104-sideload.apk`.
- **Companion 671 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.

## Release 1.0.0-beta.6

- Mijn-sync: de TV-bibliotheek is leidend voor IPTV-favorieten en kijkposities.
- `LibraryStore.syncFromTv` verwijdert lokale IPTV-items en demo-/loopback-items die niet
  in de TV-lijst staan; echte TV-items worden overgenomen.
- TV `IptvLibrary.removeDemoEntries` ruimt oude demo-/loopback-favorieten en -resumes op;
  `CompanionLibraryHost.apply` weigert nieuwe demo-items.
- `versionCode` 105, `versionName` 1.0.0-beta.6; TV `versionCode` 78, beta.75.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.6-build105-sideload.apk`.
- **Companion 671 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.

## Release 1.0.0-beta.7

- Remote-statuskaart: kanaalnaam en programmatitel mogen doorlopen; tijdlijn en "Straks"
  staan onder elkaar; minder afkapping.
- Tuner: de TV pollt de echte receiver-tuners (`/api/about`) als de eigen-tunerstatus
  leeg is; de telefoon toont `Tuner —` als het aantal onbekend is.
- `versionCode` 106, `versionName` 1.0.0-beta.7; TV `versionCode` 79, beta.76.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.7-build106-sideload.apk`.
- **Companion 671 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.

## Release 1.0.0-beta.8

- Mijn toont alleen films/series: Verder kijken, Favorieten en Opnames op de ontvanger.
  Live-zenders en losse recente streams zijn verborgen.
- TV stuurt de receiver-opnames uit OpenWebif `/api/movielist` mee in `state.library`;
  de telefoon speelt ze via de opname-URL en Basic-auth indien nodig.
- `versionCode` 107, `versionName` 1.0.0-beta.8; TV `versionCode` 80, beta.77.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.8-build107-sideload.apk`.
- **Companion 671 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.

## Release 1.0.0-beta.9

- TV-brug bouwt IPTV-URL's voor favorieten en kijkposities die in de TV-database geen URL hebben
  (Xtream `movie`/`episode`), filtert torrents/lege titels en stuurt receiver-opnames mee.
- Mijn op de telefoon toont alleen films/series (verder kijken, favorieten) en opnames;
  serie-favorieten openen hun afleveringen, live-zenders worden verborgen.
- `versionCode` 108, `versionName` 1.0.0-beta.9; TV `versionCode` 81, beta.78.
- Sideload: `dist/EnigmaCast-Companion-1.0.0-beta.9-build108-sideload.apk`.
- **Companion 671 tests, 0 failures, 1 skipped**; **TV 929 tests, 0 failures**.

## Release 1.0.0-beta.12

- Sync-API op 192.168.1.209 gecontroleerd (health OK, progress-records aanwezig).
- TV pusht de outbox nu vóór de profiel/settings-pass; alleen gewijzigde profielen worden nog gepubliceerd.
- TV slaat bij pauzeren en stoppen op; telefoon stuurt bij pauzeren, einde, sluiten en opnieuw verbinden de laatste positie.
- Verder kijken houdt op TV en telefoon één aflevering per serie; oudere afleveringen gaan als delete naar de sync-API.
- VOD-extensie via get_vod_info (voorkomt mp4/mkv 404).
- Speler: OSD verbergt na 5 seconden, tik om te tonen; VOD-tijdlijn, -10 s en +30 s; live zonder seek.
- Companion 671 tests, TV 929 tests.

## Release 1.0.0-beta.13

- Companion player recovery: limited Media3 retry policy, stall watchdog for buffering with and without active loading, retry with backoff, live restart at the live edge, VOD resume at the last position, permanent 4xx/parse errors not retried, and network-available recovery. Resets the retry budget after 30 seconds of stable playback.
- TV IPTV recovery: same retry/watchdog/network pattern for Xtream live, films and series. Live IPTV now also runs the stall ticker; STATE_ENDED on a live stream triggers a bounded reconnect.
- Version 1.0.0-beta.13 / build 112; TV 1.0.0-beta.82 / build 85.
- Companion 671 tests, 0 failures, 1 skipped; TV 929 tests, 0 failures.

## Release 1.0.0-beta.83
- TV: ACTION_SCREEN_OFF and TRIM_MEMORY_UI_HIDDEN stop every player and close stream/network connections, including PiP.
- Foreground/screen-on evicts stale OkHttp keep-alive sockets. HTTP status is logged with player errors.
- Version 1.0.0-beta.83 / build 86; TV tests 929, 0 failures.

## Release 1.0.0-beta.14
- IPTV clients evict keep-alive sockets on network changes; IPTV tab refreshes a failed source when it opens. The stored Xtream/M3U source is used without the TV bridge.
- Xtream live rows load now/next EPG per visible channel (get_short_epg) and no longer show the numeric stream id.
- Version 1.0.0-beta.14 / build 113; Companion 671 tests, 0 failures, 1 skipped.
