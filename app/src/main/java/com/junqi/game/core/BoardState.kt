package com.junqi.game.core

class BoardState {

    val board: Array<Array<Piece?>> = Array(ROWS) { arrayOfNulls(COLS) }

    fun init() {
        for (r in 0 until ROWS) for (c in 0 until COLS) board[r][c] = null
        val all = mutableListOf<Piece>()
        for ((name, rank, count) in PIECE_DEFS) {
            repeat(count) { all += Piece(name, rank, Side.RED) }
            repeat(count) { all += Piece(name, rank, Side.BLUE) }
        }
        all.shuffle()
        var idx = 0
        for (r in 0 until ROWS) {
            if (r == RIVER_ROW) continue
            for (c in 0 until COLS) {
                val pos = Pos(r, c)
                if (pos == MOUNTAIN) continue
                if (idx >= all.size) break
                all[idx].pos = pos
                board[r][c] = all[idx++]
            }
        }
    }

    // 翻开棋子，返回触发的战斗事件列表
    fun flipPiece(pos: Pos): List<BattleEvent> {
        val piece = board[pos.row][pos.col] ?: return emptyList()
        if (piece.flipped || piece.dead) return emptyList()
        piece.flipped = true
        val events = mutableListOf<BattleEvent>()
        for ((adjPos, result) in RuleEngine.checkFlipBattles(pos, board)) {
            val defender = board[adjPos.row][adjPos.col] ?: continue
            events += applyBattle(piece, defender, pos, adjPos, result)
        }
        return events
    }

    fun movePiece(from: Pos, to: Pos) {
        val piece = board[from.row][from.col] ?: return
        board[to.row][to.col] = piece
        board[from.row][from.col] = null
        piece.pos = to
    }

    fun attack(from: Pos, to: Pos): BattleEvent? {
        val attacker = board[from.row][from.col] ?: return null
        val defender = board[to.row][to.col]   ?: return null
        if (!defender.flipped) defender.flipped = true
        val result = RuleEngine.resolve(attacker, defender)
        return applyBattle(attacker, defender, from, to, result)
    }

    private fun applyBattle(
        attacker: Piece, defender: Piece,
        aPos: Pos, dPos: Pos, result: BattleResult
    ): BattleEvent {
        when (result) {
            BattleResult.ATTACKER_WINS -> {
                defender.dead = true
                board[dPos.row][dPos.col] = attacker
                board[aPos.row][aPos.col] = null
                attacker.pos = dPos
            }
            BattleResult.DEFENDER_WINS -> {
                attacker.dead = true
                board[aPos.row][aPos.col] = null
            }
            BattleResult.BOTH_DIE -> {
                attacker.dead = true
                defender.dead = true
                board[aPos.row][aPos.col] = null
                board[dPos.row][dPos.col] = null
            }
            else -> {}
        }
        return BattleEvent(aPos, dPos, attacker.name, defender.name, result)
    }

    fun getUnflipped(side: Side) = buildList {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = board[r][c]
            if (p != null && p.side == side && !p.flipped && !p.dead) add(Pos(r, c))
        }
    }

    fun getMovable(side: Side) = buildList {
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = board[r][c]
            if (p != null && p.side == side && p.canMove)
                if (RuleEngine.getValidMoves(Pos(r, c), board).isNotEmpty()) add(Pos(r, c))
        }
    }

    fun deepCopy(): BoardState {
        val copy = BoardState()
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = board[r][c]
            copy.board[r][c] = p?.copy(pos = p.pos.copy())
        }
        return copy
    }
}
