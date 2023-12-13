package io.agora.videoloaderapi.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import io.agora.videoloaderapi.AGSlicingType
import io.agora.videoloaderapi.AGUIType
import io.agora.videoloaderapi.AgoraApplication
import io.agora.videoloaderapi.R
import io.agora.videoloaderapi.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val mViewBinding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mViewBinding.root)

        mViewBinding.btEnter.setOnClickListener {
            AgoraApplication.the()?.let { application ->
                val intent = Intent()
                intent.setClass(application, RoomListActivity::class.java)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "startActivity failed: $e")
                }
            }
        }

        // 选择秒切UI模式
        when (AgoraApplication.the()?.uiMode) {
            AGUIType.ViewPager -> {
                mViewBinding.spUIMode.setSelection(0)
            }
            AGUIType.RecycleView -> {
                mViewBinding.spUIMode.setSelection(1)
            }
            else -> {}
        }
        mViewBinding.spUIMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                when (p2) {
                    0 -> {
                        mViewBinding.tvUIModeShow.setText(R.string.show_ui_type_view_pager)
                        AgoraApplication.the()?.uiMode = AGUIType.ViewPager
                    }
                    1 -> {
                        mViewBinding.tvUIModeShow.setText(R.string.show_ui_recycle_view)
                        AgoraApplication.the()?.uiMode =  AGUIType.RecycleView
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        // 是否开启prejoin模式
        mViewBinding.cbSwitch.isChecked = AgoraApplication.the()?.needPreJoin == true
        mViewBinding.cbSwitch.setOnCheckedChangeListener { _, isChecked ->
            AgoraApplication.the()?.let {
                it.needPreJoin = isChecked
            }
        }

        // 选择视频出图模式
        when (AgoraApplication.the()?.sliceMode) {
            AGSlicingType.VISIBLE -> {
                mViewBinding.spSliceMode.setSelection(0)
            }
            AGSlicingType.END_SCROLL -> {
                mViewBinding.spSliceMode.setSelection(1)
            }
            else -> {}
        }
        mViewBinding.spSliceMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                when (p2) {
                    0 -> {
                        mViewBinding.tvSliceModeShow.setText(R.string.show_slicing_type_visible)
                        AgoraApplication.the()?.sliceMode = AGSlicingType.VISIBLE
                    }
                    1 -> {
                        mViewBinding.tvSliceModeShow.setText(R.string.show_slicing_type_end)
                        AgoraApplication.the()?.sliceMode = AGSlicingType.END_SCROLL
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }
    }
}