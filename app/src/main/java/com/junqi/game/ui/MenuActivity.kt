package com.junqi.game.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.junqi.game.core.Difficulty
import com.junqi.game.core.GameMode
import com.junqi.game.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private var selectedDiff = Difficulty.SOLDIER

    private val DIFF_NAMES = arrayOf("新兵", "士兵", "军官", "将领", "司令")
    private val DIFF_DESC  = arrayOf(
        "完全随机，适合初学",
        "简单策略，会攻击可吃目标",
        "有防守意识，会躲避强敌",
        "概率推算，主动追击高价值目标",
        "深度搜索，挑战极限"
    )
    private val DIFF_STARS = arrayOf("★☆☆☆☆", "★★☆☆☆", "★★★☆☆", "★★★★☆", "★★★★★")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        selectDiff(1) // 默认士兵
        showMainPanel()
    }

    private fun setupButtons() {
        binding.btnVsAI.setOnClickListener       { showDiffPanel() }
        binding.btnLocalTwo.setOnClickListener   { launchGame(GameMode.LOCAL_TWO) }
        binding.btnStartGame.setOnClickListener  { launchGame(GameMode.VS_AI) }
        binding.btnBackFromDiff.setOnClickListener { showMainPanel() }

        val diffBtns = listOf(
            binding.btnDiff0, binding.btnDiff1, binding.btnDiff2,
            binding.btnDiff3, binding.btnDiff4
        )
        diffBtns.forEachIndexed { idx, btn ->
            btn.text = DIFF_STARS[idx]
            btn.setOnClickListener { selectDiff(idx) }
        }
    }

    private fun selectDiff(idx: Int) {
        selectedDiff = Difficulty.entries[idx]
        binding.tvDiffName.text  = DIFF_NAMES[idx]
        binding.tvDiffDesc.text  = DIFF_DESC[idx]

        val diffBtns = listOf(
            binding.btnDiff0, binding.btnDiff1, binding.btnDiff2,
            binding.btnDiff3, binding.btnDiff4
        )
        diffBtns.forEachIndexed { i, btn ->
            btn.isSelected = i == idx
            btn.setBackgroundColor(
                if (i == idx) 0xFF8B1A10.toInt() else 0xFF333333.toInt()
            )
        }
    }

    private fun launchGame(mode: GameMode) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("mode", mode)
        intent.putExtra("diff", selectedDiff)
        startActivity(intent)
    }

    private fun showMainPanel() {
        binding.panelMain.visibility = android.view.View.VISIBLE
        binding.panelDiff.visibility = android.view.View.GONE
    }

    private fun showDiffPanel() {
        binding.panelMain.visibility = android.view.View.GONE
        binding.panelDiff.visibility = android.view.View.VISIBLE
    }
}
