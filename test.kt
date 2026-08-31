import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun main() {
    val text = "This is the first predefined line for testing.\nThis is the second line with some more text.\nAnd this is the third line to complete\nthe initial setup."
    val caret = 111 // 'A' in 'And this'
    val anchor = 111
    val newCaret = 60 // 'T' in 'This is the second'
    
    // Simulate line mode UP
    // line 3 start = 91, end = 129
    // line 2 start = 45, end = 90
    val caretLine = 2
    val anchorLine = 3
    
    val snappedCaret = 45
    val snappedAnchor = 129 // end of line 3
    
    val range = TextRange(snappedAnchor, snappedCaret)
    println("min: ${range.min}, max: ${range.max}")
    println("selected: [${text.substring(range.min, range.max)}]")
}
