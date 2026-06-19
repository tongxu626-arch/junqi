package com.junqi.game.core

object RuleEngine {

    fun getCellType(pos: Pos): CellType = when {
        pos == MOUNTAIN         -> CellType.MOUNTAIN
        pos.row == RIVER_ROW    -> CellType.RIVER
        pos in CAMPS            -> CellType.CAMP
        pos in HQS              -> CellType.HQ
        else                    -> CellType.NORMAL
    }

    fun isRailway(pos: Pos) =
        pos.row in RAILWAY_ROWS || pos.col in RAILWAY_COLS

    fun isValidCell(pos: Pos) =
        pos.row in 0 until ROWS && pos.col in 0 until COLS &&
        pos != MOUNTAIN && pos.row != RIVER_ROW

    fun adjacent(pos: Pos): List<Pos> =
        listOf(Pos(pos.row-1,pos.col), Pos(pos.row+1,pos.col),
               Pos(pos.row,pos.col-1), Pos(pos.row,pos.col+1))
            .filter { isValidCell(it) }

    // 战斗判定
    fun resolve(attacker: Piece, defender: Piece): BattleResult {
        val a = attacker.rank
        val d = defender.rank
        if (a == RANK_BOMB || d == RANK_BOMB) return BattleResult.BOTH_DIE
        if (d == RANK_MINE) return if (a == RANK_ENGINEER)
            BattleResult.ATTACKER_WINS else BattleResult.DEFENDER_WINS
        if (d == RANK_FLAG) return BattleResult.ATTACKER_WINS
        if (a == RANK_FLAG) return BattleResult.INVALID
        return when {
            a > d -> BattleResult.ATTACKER_WINS
            a < d -> BattleResult.DEFENDER_WINS
            else  -> BattleResult.BOTH_DIE
        }
    }

    // 获取合法移动目标
    fun getValidMoves(from: Pos, board: Array<Array<Piece?>>): List<Pos> {
        val piece = board[from.row][from.col] ?: return emptyList()
        if (!piece.canMove) return emptyList()
        val result = mutableListOf<Pos>()

        // 普通相邻移动
        for (nb in adjacent(from)) {
            val target = board[nb.row][nb.col]
            if (target == null || target.dead) { result += nb; continue }
            if (target.side != piece.side) result += nb
        }

        // 工兵铁路滑行
        if (piece.rank == RANK_ENGINEER && isRailway(from)) {
            result += getRailwayMoves(from, board, piece.side)
        }

        return result.distinct()
    }

    private fun getRailwayMoves(from: Pos, board: Array<Array<Piece?>>, side: Side): List<Pos> {
        val result = mutableListOf<Pos>()
        val dirs = listOf(Pos(-1,0), Pos(1,0), Pos(0,-1), Pos(0,1))
        for (dir in dirs) {
            var r = from.row + dir.row
            var c = from.col + dir.col
            while (r in 0 until ROWS && c in 0 until COLS) {
                val pos = Pos(r, c)
                if (!isValidCell(pos)) break
                if (!isRailway(pos)) break
                val p = board[r][c]
                if (p != null && !p.dead) {
                    if (p.side != side) result += pos
                    break
                }
                result += pos
                r += dir.row; c += dir.col
            }
        }
        return result
    }

    // 翻棋后触发的四邻战斗
    fun checkFlipBattles(flipped: Pos, board: Array<Array<Piece?>>): List<Pair<Pos, BattleResult>> {
        val piece = board[flipped.row][flipped.col] ?: return emptyList()
        val battles = mutableListOf<Pair<Pos, BattleResult>>()
        for (nb in adjacent(flipped)) {
            val adj = board[nb.row][nb.col] ?: continue
            if (!adj.flipped || adj.dead || adj.side == piece.side) continue
            if (getCellType(nb) == CellType.CAMP) continue // 行营免疫
            battles += Pair(nb, resolve(piece, adj))
        }
        return battles
    }

    fun isFlagCaptured(board: Array<Array<Piece?>>, side: Side): Boolean {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = board[r][c]
            if (p != null && p.side == side && p.rank == RANK_FLAG && p.dead) return true
        }
        return false
    }

    fun hasAnyMove(board: Array<Array<Piece?>>, side: Side): Boolean {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = board[r][c] ?: continue
            if (p.side != side || p.dead) continue
            if (!p.flipped) return true // 还有未翻开的棋子
            if (getValidMoves(Pos(r, c), board).isNotEmpty()) return true
        }
        return false
    }
}
