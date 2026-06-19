package com.junqi.game.core

import android.graphics.Color

// ── 棋子 ──────────────────────────────────────────────
enum class Side { RED, BLUE }

enum class BattleResult { ATTACKER_WINS, DEFENDER_WINS, BOTH_DIE, INVALID }

enum class CellType { NORMAL, CAMP, HQ, MOUNTAIN, RIVER }

enum class Difficulty { ROOKIE, SOLDIER, OFFICER, GENERAL, COMMANDER }

enum class GameMode { VS_AI, LOCAL_TWO }

data class Pos(val row: Int, val col: Int)

data class Piece(
    val name: String,
    val rank: Int,
    val side: Side,
    var flipped: Boolean = false,
    var dead: Boolean = false,
    var pos: Pos = Pos(0, 0)
) {
    val canMove get() = flipped && !dead && rank != RANK_FLAG && rank != RANK_MINE
}

// ── 常量 ──────────────────────────────────────────────
const val ROWS = 12
const val COLS = 5

const val RANK_FLAG     = -1
const val RANK_MINE     =  0
const val RANK_ENGINEER =  2
const val RANK_BOMB     = 11

val RIVER_ROW = 5

val RAILWAY_ROWS = setOf(1, 10)
val RAILWAY_COLS = setOf(0, 4)

val MOUNTAIN = Pos(5, 2)

val CAMPS = setOf(
    Pos(2,1), Pos(2,3), Pos(3,2), Pos(4,1), Pos(4,3),
    Pos(7,1), Pos(7,3), Pos(8,2), Pos(9,1), Pos(9,3)
)

val HQS = setOf(
    Pos(0,1), Pos(0,3),
    Pos(11,1), Pos(11,3)
)

// ── 棋子定义 ──────────────────────────────────────────
val PIECE_DEFS = listOf(
    Triple("司令", 10, 1), Triple("军长",  9, 1),
    Triple("师长",  8, 2), Triple("旅长",  7, 2),
    Triple("团长",  6, 2), Triple("营长",  5, 2),
    Triple("连长",  4, 3), Triple("排长",  3, 3),
    Triple("工兵",  2, 3), Triple("炸弹", 11, 2),
    Triple("地雷",  0, 3), Triple("军旗", -1, 1)
)

// ── 颜色 ──────────────────────────────────────────────
object GameColors {
    val BOARD_BG       = Color.parseColor("#D4A83A")
    val BOARD_LINE     = Color.parseColor("#A07828")
    val RIVER_BG       = Color.parseColor("#B8861E")
    val RIVER_TEXT     = Color.parseColor("#F5D070")
    val MOUNTAIN_BG    = Color.parseColor("#8A6010")
    val MOUNTAIN_TEXT  = Color.parseColor("#5A3A05")
    val RAILWAY_LINE   = Color.parseColor("#222222")
    val CAMP_BORDER    = Color.parseColor("#3A7A20")
    val HQ_BORDER      = Color.parseColor("#1A5010")
    val PIECE_BACK     = Color.parseColor("#3A6A20")
    val PIECE_BACK_DOT = Color.parseColor("#2A5015")
    val PIECE_RED      = Color.parseColor("#E8621A")
    val PIECE_RED_BDR  = Color.parseColor("#B04010")
    val PIECE_BLUE     = Color.parseColor("#2255AA")
    val PIECE_BLUE_BDR = Color.parseColor("#112266")
    val PIECE_FLAG     = Color.parseColor("#1A4A10")
    val PIECE_FLAG_TXT = Color.parseColor("#FFD700")
    val PIECE_TEXT     = Color.WHITE
    val SELECT_RING    = Color.parseColor("#FFD700")
    val HINT_RING      = Color.parseColor("#FFD70066")
}

// ── 战斗事件 ──────────────────────────────────────────
data class BattleEvent(
    val attackerPos: Pos,
    val defenderPos: Pos,
    val attackerName: String,
    val defenderName: String,
    val result: BattleResult
) {
    fun toMessage(): String = when (result) {
        BattleResult.ATTACKER_WINS ->
            if (defenderName == "地雷") "工兵排雷成功！"
            else "$attackerName 击败 $defenderName"
        BattleResult.DEFENDER_WINS ->
            if (defenderName == "地雷") "$attackerName 踩雷阵亡！"
            else "$defenderName 击败 $attackerName"
        BattleResult.BOTH_DIE -> "炸弹！双方同归于尽"
        BattleResult.INVALID  -> ""
    }
}
