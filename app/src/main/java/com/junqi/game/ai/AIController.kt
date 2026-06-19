package com.junqi.game.ai

import com.junqi.game.core.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

sealed class AIAction {
    object None : AIAction()
    data class Flip(val pos: Pos) : AIAction()
    data class Move(val from: Pos, val to: Pos) : AIAction()
}

class AIController(private val diff: Difficulty, private val side: Side) {

    fun getAction(state: BoardState): AIAction = when (diff) {
        Difficulty.ROOKIE    -> actRookie(state)
        Difficulty.SOLDIER   -> actSoldier(state)
        Difficulty.OFFICER   -> actOfficer(state)
        Difficulty.GENERAL   -> actGeneral(state)
        Difficulty.COMMANDER -> actCommander(state)
    }

    // ── 新兵：完全随机 ────────────────────────────────
    private fun actRookie(state: BoardState): AIAction {
        val unflipped = state.getUnflipped(side)
        val movable   = state.getMovable(side)
        if (movable.isNotEmpty() && Random.nextBoolean()) {
            val from  = movable.random()
            val moves = RuleEngine.getValidMoves(from, state.board)
            if (moves.isNotEmpty()) return AIAction.Move(from, moves.random())
        }
        if (unflipped.isNotEmpty()) return AIAction.Flip(unflipped.random())
        if (movable.isNotEmpty()) {
            val from  = movable.random()
            val moves = RuleEngine.getValidMoves(from, state.board)
            if (moves.isNotEmpty()) return AIAction.Move(from, moves.random())
        }
        return AIAction.None
    }

    // ── 士兵：优先攻击可吃目标 ────────────────────────
    private fun actSoldier(state: BoardState): AIAction {
        val movable = state.getMovable(side).shuffled()
        for (from in movable) {
            val piece = state.board[from.row][from.col] ?: continue
            for (to in RuleEngine.getValidMoves(from, state.board)) {
                val target = state.board[to.row][to.col] ?: continue
                if (!target.flipped || target.side == side) continue
                if (RuleEngine.resolve(piece, target) == BattleResult.ATTACKER_WINS)
                    return AIAction.Move(from, to)
            }
        }
        return actRookie(state)
    }

    // ── 军官：记忆 + 躲避强敌 ─────────────────────────
    private fun actOfficer(state: BoardState): AIAction {
        val movable = state.getMovable(side).shuffled()

        // 先找能吃的
        for (from in movable) {
            val piece = state.board[from.row][from.col] ?: continue
            for (to in RuleEngine.getValidMoves(from, state.board)) {
                val target = state.board[to.row][to.col] ?: continue
                if (!target.flipped || target.side == side) continue
                if (RuleEngine.resolve(piece, target) == BattleResult.ATTACKER_WINS)
                    return AIAction.Move(from, to)
            }
        }

        // 移动到安全位置
        for (from in movable) {
            val piece = state.board[from.row][from.col] ?: continue
            for (to in RuleEngine.getValidMoves(from, state.board)) {
                val target = state.board[to.row][to.col]
                if (target != null) continue // 只移到空格
                if (!isThreatened(to, piece, state))
                    return AIAction.Move(from, to)
            }
        }

        return actRookie(state)
    }

    // ── 将领：概率推算 + 追击高价值 ──────────────────
    private fun actGeneral(state: BoardState): AIAction {
        val movable = state.getMovable(side).shuffled()
        var bestAction: AIAction = AIAction.None
        var bestRank = Int.MIN_VALUE

        for (from in movable) {
            val piece = state.board[from.row][from.col] ?: continue
            for (to in RuleEngine.getValidMoves(from, state.board)) {
                val target = state.board[to.row][to.col] ?: continue
                if (target.side == side) continue
                if (target.flipped) {
                    val res = RuleEngine.resolve(piece, target)
                    if (res == BattleResult.ATTACKER_WINS && target.rank > bestRank) {
                        bestRank = target.rank
                        bestAction = AIAction.Move(from, to)
                    }
                } else {
                    val avgRank = estimateAvgUnflipped(state)
                    if (piece.rank > avgRank + 2)
                        bestAction = AIAction.Move(from, to)
                }
            }
        }

        if (bestAction !is AIAction.None) return bestAction
        return actOfficer(state)
    }

    // ── 司令：Minimax 深度3 ───────────────────────────
    private fun actCommander(state: BoardState): AIAction {
        var bestAction: AIAction = AIAction.None
        var bestScore = Float.NEGATIVE_INFINITY

        for (action in getAllActions(state, side)) {
            val copy = state.deepCopy()
            applyAction(copy, action)
            val score = minimax(copy, 2, false, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
            if (score > bestScore) { bestScore = score; bestAction = action }
        }

        return if (bestAction !is AIAction.None) bestAction else actGeneral(state)
    }

    private fun minimax(state: BoardState, depth: Int, isMax: Boolean, alpha: Float, beta: Float): Float {
        if (RuleEngine.isFlagCaptured(state.board, side))                   return -10000f
        val opp = if (side == Side.RED) Side.BLUE else Side.RED
        if (RuleEngine.isFlagCaptured(state.board, opp))                    return  10000f
        if (depth == 0)                                                      return evaluate(state)

        val current = if (isMax) side else opp
        val actions = getAllActions(state, current)
        if (actions.isEmpty()) return evaluate(state)

        var a = alpha; var b = beta
        return if (isMax) {
            var best = Float.NEGATIVE_INFINITY
            for (action in actions) {
                val copy = state.deepCopy()
                applyAction(copy, action)
                val score = minimax(copy, depth - 1, false, a, b)
                best = max(best, score); a = max(a, score)
                if (b <= a) break
            }
            best
        } else {
            var best = Float.POSITIVE_INFINITY
            for (action in actions) {
                val copy = state.deepCopy()
                applyAction(copy, action)
                val score = minimax(copy, depth - 1, true, a, b)
                best = min(best, score); b = min(b, score)
                if (b <= a) break
            }
            best
        }
    }

    private fun evaluate(state: BoardState): Float {
        val opp = if (side == Side.RED) Side.BLUE else Side.RED
        var score = 0f
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = state.board[r][c] ?: continue
            if (p.dead) continue
            val v = pieceValue(p.rank)
            if (p.side == side) score += v else score -= v
        }
        return score
    }

    private fun pieceValue(rank: Int) = when (rank) {
        RANK_FLAG -> 1000f
        RANK_MINE ->    5f
        RANK_ENGINEER-> 8f
        RANK_BOMB ->   15f
        else      -> rank * 10f
    }

    private fun getAllActions(state: BoardState, s: Side): List<AIAction> = buildList {
        state.getUnflipped(s).forEach { add(AIAction.Flip(it)) }
        state.getMovable(s).forEach { from ->
            RuleEngine.getValidMoves(from, state.board).forEach { to ->
                add(AIAction.Move(from, to))
            }
        }
    }

    private fun applyAction(state: BoardState, action: AIAction) {
        when (action) {
            is AIAction.Flip -> state.flipPiece(action.pos)
            is AIAction.Move -> {
                val target = state.board[action.to.row][action.to.col]
                if (target == null || target.dead) state.movePiece(action.from, action.to)
                else state.attack(action.from, action.to)
            }
            else -> {}
        }
    }

    private fun isThreatened(pos: Pos, mover: Piece, state: BoardState): Boolean {
        for (nb in RuleEngine.adjacent(pos)) {
            val adj = state.board[nb.row][nb.col] ?: continue
            if (!adj.flipped || adj.dead || adj.side == side) continue
            if (RuleEngine.resolve(adj, mover) == BattleResult.ATTACKER_WINS) return true
        }
        return false
    }

    private fun estimateAvgUnflipped(state: BoardState): Float {
        var total = 0f; var count = 0
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val p = state.board[r][c]
            if (p != null && !p.flipped && !p.dead) { total += maxOf(0, p.rank); count++ }
        }
        return if (count > 0) total / count else 4f
    }
}
