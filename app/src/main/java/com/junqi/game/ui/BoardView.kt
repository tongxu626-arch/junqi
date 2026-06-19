package com.junqi.game.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.junqi.game.core.*

class BoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // 外部回调
    var onCellTapped: ((Int, Int) -> Unit)? = null

    // 数据
    var boardState: BoardState? = null
    var selectedPos: Pos? = null
    var validMoves: List<Pos> = emptyList()

    // 布局尺寸（由onSizeChanged计算）
    private var cellSize   = 0f
    private var cellGap    = 0f
    private var riverH     = 0f
    private var boardLeft  = 0f
    private var boardTop   = 0f

    // 预计算每行的Y起始位置
    private val rowY = FloatArray(ROWS + 1)

    // Paint对象复用
    private val paintBg     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLine   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintRail   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintPiece  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintSel    = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintHint   = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF       = RectF()

    init {
        paintLine.style   = Paint.Style.STROKE
        paintLine.strokeWidth = 1.5f
        paintLine.color   = GameColors.BOARD_LINE

        paintRail.style   = Paint.Style.STROKE
        paintRail.strokeWidth = 2.5f
        paintRail.color   = GameColors.RAILWAY_LINE
        paintRail.pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)

        paintBorder.style = Paint.Style.STROKE
        paintBorder.strokeWidth = 2f

        paintText.textAlign = Paint.Align.CENTER
        paintText.isFakeBoldText = true
        paintText.color = GameColors.PIECE_TEXT

        paintSel.style  = Paint.Style.STROKE
        paintSel.strokeWidth = 3f
        paintSel.color  = GameColors.SELECT_RING

        paintHint.style = Paint.Style.STROKE
        paintHint.strokeWidth = 2.5f
        paintHint.color = GameColors.HINT_RING
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalcLayout(w.toFloat(), h.toFloat())
    }

    private fun recalcLayout(w: Float, h: Float) {
        cellGap  = w * 0.015f
        // 楚河汉界行高 = 正常格子的0.7
        // 总高 = 11 * cellSize + 1 * riverH + 12 * cellGap，riverH = 0.7*cellSize
        // w = 5 * cellSize + 4 * cellGap + padding*2
        val hPad = w * 0.02f
        cellSize = (w - hPad * 2 - 4 * cellGap) / 5f
        riverH   = cellSize * 0.65f
        boardLeft = hPad

        val totalH = 11 * cellSize + riverH + 12 * cellGap
        boardTop   = (h - totalH) / 2f

        var y = boardTop
        for (r in 0 until ROWS) {
            rowY[r] = y
            y += if (r == RIVER_ROW) riverH + cellGap else cellSize + cellGap
        }
        rowY[ROWS] = y
    }

    private fun cellLeft(col: Int) = boardLeft + col * (cellSize + cellGap)
    private fun cellTop(row: Int)  = rowY[row]
    private fun cellHeight(row: Int) = if (row == RIVER_ROW) riverH else cellSize

    // 把像素坐标转换为棋盘行列
    private fun pixelToCell(x: Float, y: Float): Pos? {
        for (r in 0 until ROWS) {
            if (r == RIVER_ROW) continue
            val top = cellTop(r)
            val bot = top + cellHeight(r)
            if (y < top || y > bot) continue
            for (c in 0 until COLS) {
                val left  = cellLeft(c)
                val right = left + cellSize
                if (x in left..right) return Pos(r, c)
            }
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val pos = pixelToCell(event.x, event.y)
            if (pos != null) onCellTapped?.invoke(pos.row, pos.col)
        }
        return true
    }

    // ── 绘制 ─────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bs = boardState ?: return
        drawBackground(canvas)
        drawGridLines(canvas)
        drawRailways(canvas)
        drawRiverRow(canvas)
        drawPieces(canvas, bs)
        drawSelectionAndHints(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        paintBg.color = GameColors.BOARD_BG
        val totalW = 5 * cellSize + 4 * cellGap
        val totalH = rowY[ROWS] - boardTop - cellGap
        canvas.drawRoundRect(
            boardLeft - 4, boardTop - 4,
            boardLeft + totalW + 4, boardTop + totalH + 4,
            8f, 8f, paintBg
        )
    }

    private fun drawGridLines(canvas: Canvas) {
        for (r in 0 until ROWS) {
            if (r == RIVER_ROW) continue
            val top  = cellTop(r)
            val bot  = top + cellHeight(r)
            for (c in 0 until COLS) {
                val left  = cellLeft(c)
                val right = left + cellSize
                val pos   = Pos(r, c)
                if (pos == MOUNTAIN) {
                    // 山格：深色背景
                    paintBg.color = GameColors.MOUNTAIN_BG
                    canvas.drawRect(left, top, right, bot, paintBg)
                    paintText.textSize = cellSize * 0.28f
                    paintText.color = GameColors.MOUNTAIN_TEXT
                    canvas.drawText("山", left + cellSize / 2, top + cellHeight(r) / 2 + paintText.textSize / 3, paintText)
                    paintText.color = GameColors.PIECE_TEXT
                    continue
                }
                // 普通格背景
                paintBg.color = GameColors.BOARD_BG
                canvas.drawRect(left, top, right, bot, paintBg)

                when (RuleEngine.getCellType(pos)) {
                    CellType.CAMP -> drawCamp(canvas, left, top, right, bot)
                    CellType.HQ   -> drawHQ(canvas, left, top, right, bot)
                    else          -> {}
                }

                // 格子连线（军棋走线）
                paintLine.color = GameColors.BOARD_LINE
                paintLine.strokeWidth = 1f
                drawCellConnections(canvas, r, c, left, top, right, bot)
            }
        }
    }

    private fun drawCamp(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val cx = (l + r) / 2; val cy = (t + b) / 2
        val radius = (r - l) * 0.38f
        paintBorder.color = GameColors.CAMP_BORDER
        paintBorder.strokeWidth = 2f
        canvas.drawCircle(cx, cy, radius, paintBorder)
        canvas.drawCircle(cx, cy, radius * 0.6f, paintBorder)
    }

    private fun drawHQ(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val pad = (r - l) * 0.1f
        paintBorder.color = GameColors.HQ_BORDER
        paintBorder.strokeWidth = 2f
        canvas.drawRect(l + pad, t + pad, r - pad, b - pad, paintBorder)
    }

    // 标准军棋走线（斜线连接行营等）
    private fun drawCellConnections(canvas: Canvas, row: Int, col: Int,
        l: Float, t: Float, r: Float, b: Float) {
        val cx = (l + r) / 2; val cy = (t + b) / 2
        paintLine.strokeWidth = 1f

        // 横线连接
        if (col < COLS - 1) {
            val nr = cellLeft(col + 1) + cellSize / 2
            canvas.drawLine(cx, cy, nr, cy, paintLine)
        }
        // 竖线连接
        if (row < ROWS - 1 && row + 1 != RIVER_ROW) {
            val nb = cellTop(row + 1) + cellHeight(row + 1) / 2
            canvas.drawLine(cx, cy, cx, nb, paintLine)
        }

        // 斜线（行营与相邻格之间）
        val pos = Pos(row, col)
        if (pos in CAMPS || shouldHaveDiagonal(row, col)) {
            drawDiagonals(canvas, row, col, cx, cy)
        }
    }

    private fun shouldHaveDiagonal(r: Int, c: Int): Boolean {
        // 标准军棋中，特定格子有斜线（大本营附近、行营对角线）
        val diagCells = setOf(
            Pos(0,0), Pos(0,2), Pos(0,4),
            Pos(1,1), Pos(1,3),
            Pos(11,0), Pos(11,2), Pos(11,4),
            Pos(10,1), Pos(10,3)
        )
        return Pos(r, c) in diagCells
    }

    private fun drawDiagonals(canvas: Canvas, row: Int, col: Int, cx: Float, cy: Float) {
        val dirs = listOf(Pos(-1,-1), Pos(-1,1), Pos(1,-1), Pos(1,1))
        for (d in dirs) {
            val nr = row + d.row; val nc = col + d.col
            if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) continue
            if (nr == RIVER_ROW) continue
            val nPos = Pos(nr, nc)
            if (nPos == MOUNTAIN) continue
            val nx = cellLeft(nc) + cellSize / 2
            val ny = cellTop(nr) + cellHeight(nr) / 2
            canvas.drawLine(cx, cy, nx, ny, paintLine)
        }
    }

    private fun drawRailways(canvas: Canvas) {
        // 横向铁路
        for (rr in RAILWAY_ROWS) {
            val y = cellTop(rr) + cellHeight(rr) / 2
            val x0 = cellLeft(0) + cellSize / 2
            val x1 = cellLeft(COLS - 1) + cellSize / 2
            canvas.drawLine(x0, y, x1, y, paintRail)
        }
        // 纵向铁路（跳过楚河汉界行）
        for (rc in RAILWAY_COLS) {
            val x = cellLeft(rc) + cellSize / 2
            for (r in 0 until ROWS - 1) {
                if (r == RIVER_ROW || r + 1 == RIVER_ROW) continue
                val y0 = cellTop(r) + cellHeight(r) / 2
                val y1 = cellTop(r + 1) + cellHeight(r + 1) / 2
                canvas.drawLine(x, y0, x, y1, paintRail)
            }
        }
    }

    private fun drawRiverRow(canvas: Canvas) {
        val top   = cellTop(RIVER_ROW)
        val bot   = top + riverH
        val left  = cellLeft(0)
        val right = cellLeft(COLS - 1) + cellSize
        paintBg.color = GameColors.RIVER_BG
        canvas.drawRect(left, top, right, bot, paintBg)

        // 楚河汉界文字
        val texts = arrayOf("楚", "河", "", "汉", "界")
        paintText.textSize  = riverH * 0.45f
        paintText.color     = GameColors.RIVER_TEXT
        for (c in 0 until COLS) {
            val cx = cellLeft(c) + cellSize / 2
            val cy = (top + bot) / 2 + paintText.textSize / 3
            canvas.drawText(texts[c], cx, cy, paintText)
        }
        paintText.color = GameColors.PIECE_TEXT
    }

    private fun drawPieces(canvas: Canvas, bs: BoardState) {
        for (r in 0 until ROWS) {
            if (r == RIVER_ROW) continue
            for (c in 0 until COLS) {
                val pos   = Pos(r, c)
                if (pos == MOUNTAIN) continue
                val piece = bs.board[r][c] ?: continue
                if (piece.dead) continue
                drawPiece(canvas, piece, r, c)
            }
        }
    }

    private fun drawPiece(canvas: Canvas, piece: Piece, row: Int, col: Int) {
        val l  = cellLeft(col)
        val t  = cellTop(row)
        val sz = cellSize
        val pad = sz * 0.08f
        rectF.set(l + pad, t + pad, l + sz - pad, t + sz - pad)
        val radius = sz * 0.12f

        if (!piece.flipped) {
            // 背面：绿色圆角方块
            paintPiece.color = GameColors.PIECE_BACK
            paintPiece.style = Paint.Style.FILL
            canvas.drawRoundRect(rectF, radius, radius, paintPiece)
            // 中心小方块装饰
            val inner = sz * 0.2f
            val ix = l + sz / 2; val iy = t + sz / 2
            paintPiece.color = GameColors.PIECE_BACK_DOT
            canvas.drawRoundRect(ix - inner, iy - inner, ix + inner, iy + inner,
                radius * 0.5f, radius * 0.5f, paintPiece)
        } else {
            // 正面
            paintPiece.style = Paint.Style.FILL
            paintPiece.color = when {
                piece.rank == RANK_FLAG -> GameColors.PIECE_FLAG
                piece.side == Side.RED  -> GameColors.PIECE_RED
                else                    -> GameColors.PIECE_BLUE
            }
            canvas.drawRoundRect(rectF, radius, radius, paintPiece)

            // 描边
            paintBorder.color = when {
                piece.rank == RANK_FLAG -> GameColors.PIECE_FLAG_TXT
                piece.side == Side.RED  -> GameColors.PIECE_RED_BDR
                else                    -> GameColors.PIECE_BLUE_BDR
            }
            paintBorder.strokeWidth = sz * 0.04f
            canvas.drawRoundRect(rectF, radius, radius, paintBorder)

            // 文字
            paintText.textSize  = sz * 0.28f
            paintText.color = if (piece.rank == RANK_FLAG) GameColors.PIECE_FLAG_TXT
                              else GameColors.PIECE_TEXT
            canvas.drawText(
                if (piece.name.length > 2) piece.name.substring(0, 2) else piece.name,
                l + sz / 2, t + sz / 2 + paintText.textSize / 3, paintText
            )
        }
    }

    private fun drawSelectionAndHints(canvas: Canvas) {
        val sz  = cellSize
        val rad = sz * 0.12f

        // 合法移动提示（半透明金色边框）
        for (pos in validMoves) {
            if (pos.row == RIVER_ROW) continue
            val l = cellLeft(pos.col); val t = cellTop(pos.row)
            rectF.set(l + 2f, t + 2f, l + sz - 2f, t + sz - 2f)
            canvas.drawRoundRect(rectF, rad, rad, paintHint)
        }

        // 选中棋子（亮金色边框）
        selectedPos?.let { pos ->
            if (pos.row == RIVER_ROW) return@let
            val l = cellLeft(pos.col); val t = cellTop(pos.row)
            rectF.set(l + 1.5f, t + 1.5f, l + sz - 1.5f, t + sz - 1.5f)
            canvas.drawRoundRect(rectF, rad, rad, paintSel)
        }
    }

    fun refresh() = invalidate()
}
