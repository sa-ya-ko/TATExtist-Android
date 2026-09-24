package com.tatextist.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// M3 Expressive仕様の角丸シェイプ定義（標準M3よりも豊かで特徴的な丸み）
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), // 極小バッジや微小タグ用
    small = RoundedCornerShape(8.dp), // 小型ボタン、チップ用
    medium = RoundedCornerShape(16.dp), // カード、フローティングツールバー内ボタン用
    large = RoundedCornerShape(24.dp), // フローティングツールバー、ダイアログ用
    extraLarge = RoundedCornerShape(28.dp) // ボトムシート、大コンテナ用
)
