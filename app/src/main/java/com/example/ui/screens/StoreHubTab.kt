package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DigiRation
import com.example.data.model.DigimonCatalog
import com.example.data.model.DigimonCreature
import com.example.ui.theme.BioEmeraldHealth
import com.example.ui.theme.CyberVoidBg
import com.example.ui.theme.EvolutionGoldSecondary
import com.example.ui.theme.HoloCardElevated
import com.example.ui.theme.HoloCyanPrimary
import com.example.ui.theme.HoloSlateSurface
import com.example.ui.theme.SolarCoralHunger
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreHubTab(
    activeCreature: DigimonCreature?,
    digiCoins: Int,
    nextDigiCoinTickSec: Int,
    foodInventory: Map<DigiRation, Int>,
    tammingBerryCount: Int,
    standardMonsterPartsCount: Int = 0,
    ultraRareMonsterPartsCount: Int = 0,
    dailyLoginClaimedToday: Boolean = false,
    dailyLoginStreakDays: Int = 1,
    lastDailyLoginRewardText: String? = null,
    onClaimDailyLoginReward: () -> Unit = {},
    onBuyFood: (DigiRation, Int) -> Unit,
    onBuyTammingBerries: (Int) -> Unit,
    onSellStandardPart: (Int) -> Unit = {},
    onSellUltraRarePart: (Int) -> Unit = {},
    onSellAllParts: () -> Unit = {},
    onFeedCreatureFromBag: (DigiRation) -> Unit,
    onOpenWildTamingModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextCoinMins = nextDigiCoinTickSec / 60
    val nextCoinSecs = nextDigiCoinTickSec % 60
    val timerFormatted = "%d:%02d".format(Locale.US, nextCoinMins, nextCoinSecs)
    val totalFoodOwned = foodInventory.values.sum()
    val totalPartsCount = standardMonsterPartsCount + ultraRareMonsterPartsCount
    val totalPartsCoinValue =
        (standardMonsterPartsCount * DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS) +
            (ultraRareMonsterPartsCount * DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("store_hub_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = "Store",
                tint = EvolutionGoldSecondary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "DIGI-STORE // FOOD, BERRIES & PARTS",
                style = MaterialTheme.typography.headlineLarge,
                color = EvolutionGoldSecondary
            )
        }

        // Daily Login Reward & 7-Day Streak Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_daily_login_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (dailyLoginClaimedToday) BioEmeraldHealth.copy(alpha = 0.7f) else EvolutionGoldSecondary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY LOGIN REWARD // DAY $dailyLoginStreakDays/7 STREAK",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dailyLoginClaimedToday) BioEmeraldHealth else EvolutionGoldSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = (if (dailyLoginClaimedToday) BioEmeraldHealth else EvolutionGoldSecondary).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, if (dailyLoginClaimedToday) BioEmeraldHealth else EvolutionGoldSecondary)
                    ) {
                        Text(
                            text = if (dailyLoginClaimedToday) "CLAIMED TODAY" else "READY TO CLAIM!",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dailyLoginClaimedToday) BioEmeraldHealth else EvolutionGoldSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = if (dailyLoginClaimedToday && !lastDailyLoginRewardText.isNullOrBlank()) {
                        "✓ $lastDailyLoginRewardText • Log in again tomorrow to continue your 7-day retention streak!"
                    } else {
                        "Claim your daily login bonus once per day for a small amount of DigiCoins (10–25 Coins) or a random Berry (1–2x Tamming Berries)!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Button(
                    onClick = onClaimDailyLoginReward,
                    enabled = !dailyLoginClaimedToday,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dailyLoginClaimedToday) BioEmeraldHealth.copy(alpha = 0.25f) else EvolutionGoldSecondary,
                        contentColor = if (dailyLoginClaimedToday) BioEmeraldHealth else CyberVoidBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("store_claim_daily_login_button")
                ) {
                    Text(
                        text = if (dailyLoginClaimedToday) {
                            "✓ TODAY'S DAILY LOGIN REWARD CLAIMED"
                        } else {
                            "CLAIM DAILY LOGIN REWARD (DIGICOINS OR BERRY)"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Wallet & Bag Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_wallet_summary_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
            border = BorderStroke(1.5.dp, EvolutionGoldSecondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "BALANCE: $digiCoins DIGICOINS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = EvolutionGoldSecondary
                    )
                    Surface(
                        color = HoloCyanPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HoloCyanPrimary.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "+5 COINS IN $timerFormatted (EVERY 5M)",
                            style = MaterialTheme.typography.labelSmall,
                            color = HoloCyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = BioEmeraldHealth.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BioEmeraldHealth.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = BioEmeraldHealth,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TAMMING BERRIES IN BAG: $tammingBerryCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = BioEmeraldHealth
                            )
                        }
                    }

                    Surface(
                        color = SolarCoralHunger.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SolarCoralHunger.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = SolarCoralHunger,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FOOD RATIONS IN BAG: $totalFoodOwned",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolarCoralHunger
                            )
                        }
                    }

                    Surface(
                        color = EvolutionGoldSecondary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EvolutionGoldSecondary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "MONSTER PARTS: $totalPartsCount ($totalPartsCoinValue COINS)",
                                style = MaterialTheme.typography.labelMedium,
                                color = EvolutionGoldSecondary
                            )
                        }
                    }
                }

                Text(
                    text = "Store Rule: Buy or claim Food Rations here before feeding your pet, buy Tamming Berries (2 DigiCoins each) to capture wild monsters, or sell Monster Parts earned from battling wild monsters!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        // SECTION 0: MONSTER PARTS SALVAGE EXCHANGE (2 DIGICOINS STANDARD / 5 DIGICOINS ULTRA RARE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_monster_parts_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
            border = BorderStroke(1.5.dp, EvolutionGoldSecondary.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MONSTER PARTS SALVAGE EXCHANGE",
                            style = MaterialTheme.typography.titleMedium,
                            color = EvolutionGoldSecondary
                        )
                        Text(
                            text = "Sell parts dropped from battling wild monsters you don't want to tame: Standard Parts = ${DigimonCatalog.STANDARD_MONSTER_PART_SELL_COINS} DigiCoins each • Ultra Rare Wild Parts = ${DigimonCatalog.ULTRA_RARE_MONSTER_PART_SELL_COINS} DigiCoins each!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = EvolutionGoldSecondary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, EvolutionGoldSecondary)
                    ) {
                        Text(
                            text = "$totalPartsCount PARTS ($totalPartsCoinValue COINS)",
                            style = MaterialTheme.typography.labelLarge,
                            color = EvolutionGoldSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSellStandardPart(1) },
                        enabled = standardMonsterPartsCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HoloCyanPrimary,
                            contentColor = CyberVoidBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("store_sell_standard_part_btn")
                    ) {
                        Text(
                            text = "SELL STD ($standardMonsterPartsCount) • +2 COINS",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = { onSellUltraRarePart(1) },
                        enabled = ultraRareMonsterPartsCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EvolutionGoldSecondary,
                            contentColor = CyberVoidBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("store_sell_ultra_rare_part_btn")
                    ) {
                        Text(
                            text = "SELL ULTRA ($ultraRareMonsterPartsCount) • +5 COINS",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (totalPartsCount > 0) {
                    FilledTonalButton(
                        onClick = onSellAllParts,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BioEmeraldHealth.copy(alpha = 0.22f),
                            contentColor = BioEmeraldHealth
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("store_sell_all_parts_btn")
                    ) {
                        Text(
                            text = "SELL ALL $totalPartsCount MONSTER PARTS (+$totalPartsCoinValue DIGICOINS)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // SECTION 1: TAMMING BERRIES SHOP (2 DIGICOINS EACH)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_tamming_berry_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HoloSlateSurface),
            border = BorderStroke(1.5.dp, BioEmeraldHealth.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TAMMING BERRIES (WILD CAPTURE)",
                            style = MaterialTheme.typography.titleMedium,
                            color = BioEmeraldHealth
                        )
                        Text(
                            text = "Costs ${DigimonCatalog.TAMMING_BERRY_COIN_COST} DigiCoins per berry • Random success roll to tame 100 wild monsters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = BioEmeraldHealth.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, BioEmeraldHealth)
                    ) {
                        Text(
                            text = "IN BAG: $tammingBerryCount",
                            style = MaterialTheme.typography.labelLarge,
                            color = BioEmeraldHealth,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onBuyTammingBerries(1) },
                        enabled = digiCoins >= DigimonCatalog.TAMMING_BERRY_COIN_COST,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BioEmeraldHealth,
                            contentColor = CyberVoidBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("store_buy_1_tamming_berry_btn")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BUY 1x (2 COINS)", style = MaterialTheme.typography.labelLarge)
                    }

                    FilledTonalButton(
                        onClick = { onBuyTammingBerries(5) },
                        enabled = digiCoins >= DigimonCatalog.TAMMING_BERRY_COIN_COST * 5,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EvolutionGoldSecondary.copy(alpha = 0.22f),
                            contentColor = EvolutionGoldSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("store_buy_5_tamming_berry_btn")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BUY 5x (10 COINS)", style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedButton(
                    onClick = onOpenWildTamingModal,
                    border = BorderStroke(1.dp, HoloCyanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("store_open_wild_taming_btn")
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = HoloCyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "USE BERRIES IN WILD TAMING ($tammingBerryCount IN BAG)",
                        style = MaterialTheme.typography.labelLarge,
                        color = HoloCyanPrimary
                    )
                }
            }
        }

        // SECTION 2: PET FOOD & STAT RATIONS SHOP
        Text(
            text = "PET FOOD SHOP // BUY TO FEED ${activeCreature?.nickname?.uppercase() ?: "PARTNER"}",
            style = MaterialTheme.typography.headlineMedium,
            color = SolarCoralHunger
        )
        if (activeCreature != null) {
            Text(
                text = "Active Pet: ${activeCreature.nickname} • Satiety: ${activeCreature.hunger}% • HP: ${activeCreature.health}% • STA: ${activeCreature.stamina}% (Buy food below to add to your bag, then tap Feed!)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DigiRation.entries.forEach { ration ->
            val owned = foodInventory[ration] ?: 0
            val isFree = ration.costCoins == 0
            val canAfford = digiCoins >= ration.costCoins
            val accentColor = if (isFree) BioEmeraldHealth else SolarCoralHunger

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_food_card_${ration.name.lowercase()}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HoloCardElevated),
                border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = ration.displayName,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = ration.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = ration.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (owned > 0) BioEmeraldHealth.copy(alpha = 0.2f) else CyberVoidBg,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (owned > 0) BioEmeraldHealth else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = "IN BAG: $owned",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (owned > 0) BioEmeraldHealth else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onBuyFood(ration, 1) },
                            enabled = isFree || canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = CyberVoidBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("store_buy_food_${ration.name.lowercase()}")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFree) "CLAIM +1 (FREE)" else "BUY +1 (${ration.costCoins} COINS)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        FilledTonalButton(
                            onClick = { onFeedCreatureFromBag(ration) },
                            enabled = owned > 0 && activeCreature != null,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = HoloCyanPrimary.copy(alpha = 0.22f),
                                contentColor = HoloCyanPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("store_feed_bag_${ration.name.lowercase()}")
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (owned > 0) "FEED PET ($owned LEFT)" else "BUY FIRST TO FEED",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
