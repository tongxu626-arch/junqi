package com.junqi.game.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.junqi.game.ai.AIAction
import com.junqi.game.ai.AIController
import com.junqi.game.core.*
import com.junqi.game.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var boardState: BoardState
    private var aiController: AIController? = null

    private var gameMode     = GameMode.VS_AI
    private var currentTurn  = Side.RED
    private var selectedPos: Pos? = null
    private var validMoves   = listOf<Pos>()
    private var gameOver     = false
    private var aiThinking   = false
    private var redScore     = 0
    private var blueScore    = 0

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameMode = intent.getSerializableExtra("mode") as? GameMode ?: GameMode.VS_AI
        val diff = intent.getSerializableExtra("diff") as? Difficulty ?: Difficulty.SOLDIER

        if (gameMode == GameMode.VS_AI)
            aiController = AIController(diff, Side.BLUE)

        binding.boardView.onCellTapped = { row, col -> onCellTapped(row, col) }
        binding.btnSurrender.setOnClickListener { showSurrenderDialog() }
        binding.btnCancel.setOnClickListener    { cancelSelection() }
        binding.btnRestart.setOnClickListener   { restartGame() }

        startGame()
    }

    private fun startGame() {
        boardState   = BoardState()
        boardState.init()
        currentTurn  = Side.RED
        selectedPos  = null
        validMoves   = emptyList()
        gameOver     = false
        aiThinking   = false
        redScore     = 0
        blueScore    = 0

        binding.boardView.boardState  = boardState
        binding.boardView.selectedPos = null
        binding.boardView.validMoves  = emptyList()
        binding.boardView.refresh()

        updateTurnLabel()
        updateScore()
        setMessage("点击任意棋子翻开")
        binding.panelResult.visibility = View.GONE
    }

    private fun onCellTapped(row: Int, col: Int) {
        if (gameOver || aiThinking) return
        if (gameMode == GameMode.VS_AI && currentTurn == Side.BLUE) return

        val pos   = Pos(row, col)
        val piece = boardState.board[row][col]

        // 点击空格：尝试移动
        if (piece == null || piece.dead) {
            if (selectedPos != null) tryMove(selectedPos!!, pos)
            return
        }

        // 点击未翻开棋子
        if (!piece.flipped) {
            val sel = selectedPos
            if (sel != null) {
                val sp = boardState.board[sel.row][sel.col]
                if (sp != null && sp.side == currentTurn &&
                    RuleEngine.getValidMoves(sel, boardState.board).contains(pos)) {
                    tryAttack(sel, pos); return
                }
            }
            tryFlip(row, col); return
        }

        // 点击己方已翻开棋子：选中/切换
        if (piece.side == currentTurn) {
            if (selectedPos == pos) { cancelSelection(); return }
            selectPiece(pos); return
        }

        // 点击敌方棋子：攻击
        if (selectedPos != null) {
            val sp = boardState.board[selectedPos!!.row][selectedPos!!.col]
            if (sp != null && sp.side == currentTurn &&
                RuleEngine.getValidMoves(selectedPos!!, boardState.board).contains(pos))
                tryAttack(selectedPos!!, pos)
        }
    }

    private fun tryFlip(row: Int, col: Int) {
        val events = boardState.flipPiece(Pos(row, col))
        val pieceName = boardState.board[row][col]?.name ?: ""
        var msg = "翻开【$pieceName】"
        for (ev in events) {
            processScore(ev)
            msg = ev.toMessage()
        }
        setMessage(msg)
        cancelSelection()
        refreshBoard()
        checkGameOver()
        if (!gameOver) endTurn()
    }

    private fun tryMove(from: Pos, to: Pos) {
        if (!RuleEngine.getValidMoves(from, boardState.board).contains(to)) {
            setMessage("不能移动到该位置"); return
        }
        val target = boardState.board[to.row][to.col]
        if (target != null && !target.dead) { tryAttack(from, to); return }
        boardState.movePiece(from, to)
        setMessage("移动【${boardState.board[to.row][to.col]?.name}】")
        cancelSelection()
        refreshBoard()
        endTurn()
    }

    private fun tryAttack(from: Pos, to: Pos) {
        if (!RuleEngine.getValidMoves(from, boardState.board).contains(to)) {
            setMessage("不能攻击该位置"); return
        }
        val ev = boardState.attack(from, to) ?: return
        processScore(ev)
        setMessage(ev.toMessage())
        cancelSelection()
        refreshBoard()
        checkGameOver()
        if (!gameOver) endTurn()
    }

    private fun selectPiece(pos: Pos) {
        selectedPos = pos
        validMoves  = RuleEngine.getValidMoves(pos, boardState.board)
        binding.boardView.selectedPos = pos
        binding.boardView.validMoves  = validMoves
        binding.boardView.refresh()
        val name = boardState.board[pos.row][pos.col]?.name ?: ""
        setMessage("已选【$name】，点击目标格移动或攻击")
    }

    private fun cancelSelection() {
        selectedPos = null
        validMoves  = emptyList()
        binding.boardView.selectedPos = null
        binding.boardView.validMoves  = emptyList()
        binding.boardView.refresh()
    }

    private fun processScore(ev: com.junqi.game.core.BattleEvent) {
        when (ev.result) {
            BattleResult.ATTACKER_WINS -> {
                val winner = boardState.board[ev.defenderPos.row][ev.defenderPos.col]
                if (winner?.side == Side.RED) redScore++ else blueScore++
            }
            BattleResult.DEFENDER_WINS -> {
                val winner = boardState.board[ev.defenderPos.row][ev.defenderPos.col]
                if (winner?.side == Side.RED) redScore++ else blueScore++
            }
            else -> {}
        }
        updateScore()
    }

    private fun endTurn() {
        currentTurn = if (currentTurn == Side.RED) Side.BLUE else Side.RED
        updateTurnLabel()
        if (gameMode == GameMode.VS_AI && currentTurn == Side.BLUE) doAITurn()
    }

    private fun doAITurn() {
        aiThinking = true
        setMessage("AI 思考中...")
        handler.postDelayed({
            val action = aiController?.getAction(boardState) ?: AIAction.None
            when (action) {
                is AIAction.Flip -> {
                    val events = boardState.flipPiece(action.pos)
                    var msg = "AI 翻开棋子"
                    for (ev in events) { processScore(ev); msg = "AI：${ev.toMessage()}" }
                    setMessage(msg)
                }
                is AIAction.Move -> {
                    val target = boardState.board[action.to.row][action.to.col]
                    if (target == null || target.dead) {
                        boardState.movePiece(action.from, action.to)
                        setMessage("AI 移动棋子")
                    } else {
                        val ev = boardState.attack(action.from, action.to)
                        if (ev != null) { processScore(ev); setMessage("AI：${ev.toMessage()}") }
                    }
                }
                else -> setMessage("AI 无棋可走")
            }
            refreshBoard()
            checkGameOver()
            aiThinking = false
            if (!gameOver) {
                currentTurn = Side.RED
                updateTurnLabel()
                setMessage("轮到你了")
            }
        }, 800)
    }

    private fun checkGameOver() {
        when {
            RuleEngine.isFlagCaptured(boardState.board, Side.RED) -> showResult("蓝方夺旗，获胜！")
            RuleEngine.isFlagCaptured(boardState.board, Side.BLUE) -> showResult("红方夺旗，获胜！")
            !RuleEngine.hasAnyMove(boardState.board, currentTurn) -> {
                val winner = if (currentTurn == Side.RED) "蓝方" else "红方"
                showResult("${winner}获胜（对方无棋可走）")
            }
        }
    }

    private fun showResult(title: String) {
        gameOver = true
        binding.panelResult.visibility  = View.VISIBLE
        binding.tvResultTitle.text       = title
        binding.tvResultScore.text       = "红方：$redScore 子　蓝方：$blueScore 子"
        binding.btnPlayAgain.setOnClickListener { restartGame() }
        binding.btnBackMenu.setOnClickListener  { finish() }
    }

    private fun showSurrenderDialog() {
        AlertDialog.Builder(this)
            .setTitle("认输")
            .setMessage("确定要认输吗？")
            .setPositiveButton("确定") { _, _ ->
                val winner = if (currentTurn == Side.RED) "蓝方" else "红方"
                showResult("${winner}获胜（对方认输）")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun restartGame() {
        binding.panelResult.visibility = View.GONE
        startGame()
    }

    private fun refreshBoard() {
        binding.boardView.boardState  = boardState
        binding.boardView.selectedPos = selectedPos
        binding.boardView.validMoves  = validMoves
        binding.boardView.refresh()
    }

    private fun updateTurnLabel() {
        binding.tvTurn.text = if (currentTurn == Side.RED) "红方回合" else "蓝方回合"
        val color = if (currentTurn == Side.RED) 0xFFE8621A.toInt() else 0xFF2255AA.toInt()
        binding.tvTurn.setTextColor(color)
    }

    private fun updateScore() {
        binding.tvRedScore.text  = redScore.toString()
        binding.tvBlueScore.text = blueScore.toString()
    }

    private fun setMessage(msg: String) {
        binding.tvMessage.text = msg
    }
}
