package org.coolreader.crengine

/**
 * Mirror of CoolReader's TOCItem Java class.
 *
 * Field names must match exactly what cr3java.cpp's TOCItemAccessor accesses:
 *   mLevel, mPage, mPercent → Int
 *   mName, mPath            → String
 * Plus method addChild() which the JNI calls to build the tree.
 */
class TOCItem {
    @JvmField var mLevel: Int = 0
    @JvmField var mPage: Int = 0
    @JvmField var mPercent: Int = 0
    @JvmField var mName: String? = null
    @JvmField var mPath: String? = null

    private val mChildren: MutableList<TOCItem> = mutableListOf()

    /** Called by JNI to add a child and return it. */
    fun addChild(): TOCItem {
        val child = TOCItem()
        mChildren.add(child)
        return child
    }

    fun getChildCount(): Int = mChildren.size
    fun getChild(index: Int): TOCItem = mChildren[index]

    /** Flattened list for UI display; call on the root item. */
    fun flatten(): List<TOCItem> {
        val result = mutableListOf<TOCItem>()
        for (child in mChildren) {
            result.add(child)
            result.addAll(child.flatten())
        }
        return result
    }
}
