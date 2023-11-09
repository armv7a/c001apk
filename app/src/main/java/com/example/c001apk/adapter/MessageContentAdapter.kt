package com.example.c001apk.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.ThemeUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.R
import com.example.c001apk.logic.model.MessageResponse
import com.example.c001apk.ui.activity.CopyActivity
import com.example.c001apk.ui.activity.FeedActivity
import com.example.c001apk.ui.activity.UserActivity
import com.example.c001apk.ui.fragment.minterface.IOnLikeClickListener
import com.example.c001apk.ui.fragment.minterface.IOnReplyClickListener
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageShowUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.SpannableStringBuilderUtil
import com.example.c001apk.view.ninegridimageview.NineGridImageView
import com.example.c001apk.view.ninegridimageview.OnImageItemClickListener
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator


class MessageContentAdapter(
    private val mContext: Context,
    private val type: String,
    private val dataList: List<MessageResponse.Data>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onImageItemClickListener: OnImageItemClickListener? = null

    fun setOnImageItemClickListener(onImageItemClickListener: OnImageItemClickListener) {
        this.onImageItemClickListener = onImageItemClickListener
    }

    private var iOnLikeClickListener: IOnLikeClickListener? = null

    fun setIOnLikeReplyListener(iOnLikeClickListener: IOnLikeClickListener) {
        this.iOnLikeClickListener = iOnLikeClickListener
    }

    private lateinit var iOnReplyClickListener: IOnReplyClickListener

    fun setIOnReplyClickListener(iOnReplyClickListener: IOnReplyClickListener) {
        this.iOnReplyClickListener = iOnReplyClickListener
    }

    private var loadState = 2
    val LOADING = 1
    val LOADING_COMPLETE = 2
    val LOADING_END = 3

    @SuppressLint("NotifyDataSetChanged")
    fun setLoadState(loadState: Int) {
        this.loadState = loadState
        notifyDataSetChanged()
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val uname: TextView = view.findViewById(R.id.uname)
        var id = ""
        var uid = ""
        var entityType = ""
        var isLike = false
        val message: TextView = view.findViewById(R.id.message)
        val pubDate: TextView = view.findViewById(R.id.pubDate)
        val like: TextView = view.findViewById(R.id.like)
        val avatar: ImageView = view.findViewById(R.id.avatar)
        val reply: TextView = view.findViewById(R.id.reply)
        val device: TextView = view.findViewById(R.id.device)
        val multiImage: NineGridImageView = view.findViewById(R.id.multiImage)
        val forward: MaterialCardView = view.findViewById(R.id.forward)
        var forwardEntityType = ""
        var forwardId = ""
        var forwardUid = ""
        var forwardUname = ""
        val forwardTitle: TextView = view.findViewById(R.id.forwardTitle)
        val forwardMessage: TextView = view.findViewById(R.id.forwardMessage)
        val forward1: MaterialCardView = view.findViewById(R.id.forward1)
        var forwardId1 = ""
        var forwardUid1 = ""
        var forwardUname1: TextView = view.findViewById(R.id.forwardUname1)
        val forwardMessage1: TextView = view.findViewById(R.id.forwardMessage1)
        val forward1Pic: ShapeableImageView = view.findViewById(R.id.forward1Pic)
    }

    class FootViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val footerLayout: FrameLayout = view.findViewById(R.id.footerLayout)
        val indicator: CircularProgressIndicator = view.findViewById(R.id.indicator)
        val noMore: TextView = view.findViewById(R.id.noMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_content, parent, false)
                val viewHolder = MessageViewHolder(view)
                viewHolder.uname.setOnClickListener {
                    val intent = Intent(parent.context, UserActivity::class.java)
                    intent.putExtra("id", viewHolder.uid)
                    parent.context.startActivity(intent)
                }
                viewHolder.avatar.setOnClickListener {
                    val intent = Intent(parent.context, UserActivity::class.java)
                    intent.putExtra("id", viewHolder.uid)
                    parent.context.startActivity(intent)
                }
                viewHolder.message.setOnLongClickListener {
                    val intent = Intent(parent.context, CopyActivity::class.java)
                    intent.putExtra("text", viewHolder.message.text.toString())
                    parent.context.startActivity(intent)
                    true
                }
                viewHolder.itemView.setOnLongClickListener {
                    val intent = Intent(parent.context, CopyActivity::class.java)
                    intent.putExtra("text", viewHolder.message.text.toString())
                    parent.context.startActivity(intent)
                    true
                }
                viewHolder.itemView.setOnClickListener {
                    if (viewHolder.entityType == "feed") {
                        val intent = Intent(parent.context, FeedActivity::class.java)
                        intent.putExtra("type", "feed")
                        intent.putExtra("id", viewHolder.id)
                        intent.putExtra("uid", viewHolder.uid)
                        intent.putExtra("uname", viewHolder.uname.text)
                        parent.context.startActivity(intent)
                    }
                }
                viewHolder.message.setOnClickListener {
                    if (viewHolder.entityType == "feed") {
                        val intent = Intent(parent.context, FeedActivity::class.java)
                        intent.putExtra("type", "feed")
                        intent.putExtra("id", viewHolder.id)
                        intent.putExtra("uid", viewHolder.uid)
                        intent.putExtra("uname", viewHolder.uname.text)
                        parent.context.startActivity(intent)
                    }
                }
                viewHolder.forward.setOnClickListener {
                    if (viewHolder.forwardEntityType == "feed") {
                        val intent = Intent(parent.context, FeedActivity::class.java)
                        intent.putExtra("type", "feed")
                        intent.putExtra("id", viewHolder.forwardId)
                        intent.putExtra("uid", viewHolder.forwardUid)
                        intent.putExtra("uname", viewHolder.forwardUname)
                        parent.context.startActivity(intent)
                    }
                }
                viewHolder.forwardMessage.setOnClickListener {
                    if (viewHolder.forwardEntityType == "feed") {
                        val intent = Intent(parent.context, FeedActivity::class.java)
                        intent.putExtra("type", "feed")
                        intent.putExtra("id", viewHolder.forwardId)
                        intent.putExtra("uid", viewHolder.forwardUid)
                        intent.putExtra("uname", viewHolder.forwardUname)
                        parent.context.startActivity(intent)
                    }
                }
                viewHolder.forwardMessage.setOnLongClickListener {
                    val intent = Intent(parent.context, CopyActivity::class.java)
                    intent.putExtra("text", viewHolder.forwardMessage.text.toString())
                    parent.context.startActivity(intent)
                    true
                }
                viewHolder.like.setOnClickListener {
                    if (PrefManager.isLogin) {
                        iOnLikeClickListener?.onPostLike(
                            null,
                            viewHolder.isLike,
                            viewHolder.id,
                            viewHolder.bindingAdapterPosition
                        )
                    }
                }
                viewHolder.multiImage.apply {
                    onImageItemClickListener = this@MessageContentAdapter.onImageItemClickListener
                }
                viewHolder.forward1.setOnClickListener {
                    val intent = Intent(parent.context, FeedActivity::class.java)
                    intent.putExtra("type", "feed")
                    intent.putExtra("id", viewHolder.forwardId1)
                    intent.putExtra("uid", viewHolder.forwardUid1)
                    intent.putExtra("uname", viewHolder.forwardUname1.text)
                    parent.context.startActivity(intent)
                }
                viewHolder
            }

            -1 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_rv_footer, parent, false)
                FootViewHolder(view)
            }

            1 -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_search_user, parent, false)
                val viewHolder = AppAdapter.UserViewHolder(view)
                viewHolder.itemView.setOnClickListener {
                    val intent = Intent(parent.context, UserActivity::class.java)
                    intent.putExtra("id", viewHolder.uid)
                    parent.context.startActivity(intent)
                }
                viewHolder
            }

            else -> throw IllegalArgumentException("invalid type")
        }

    }

    override fun getItemCount() = dataList.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) -1
        else when (type) {
            "atMe" -> 0
            "atCommentMe" -> 0
            "feedLike" -> 0
            "contactsFollow" -> 1
            "list" -> 1
            else -> throw IllegalArgumentException("invalid type")
        }
    }

    @SuppressLint("SetTextI18n", "RestrictedApi", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {

            is AppAdapter.UserViewHolder -> {
                val message = dataList[position]
                holder.uid = message.fromuid
                holder.uname.text = message.fromusername
                holder.follow.text = DateUtils.fromToday(message.dateline)
                if (type == "contactsFollow")
                    holder.fans.text = "关注了你"
                ImageShowUtil.showAvatar(holder.avatar, message.fromUserAvatar)
            }

            is FootViewHolder -> {
                when (loadState) {
                    LOADING -> {
                        holder.footerLayout.visibility = View.VISIBLE
                        holder.indicator.visibility = View.VISIBLE
                        holder.indicator.isIndeterminate = true
                        holder.noMore.visibility = View.GONE

                    }

                    LOADING_COMPLETE -> {
                        holder.footerLayout.visibility = View.GONE
                        holder.indicator.visibility = View.GONE
                        holder.indicator.isIndeterminate = false
                        holder.noMore.visibility = View.GONE
                    }

                    LOADING_END -> {
                        holder.footerLayout.visibility = View.VISIBLE
                        holder.indicator.visibility = View.GONE
                        holder.indicator.isIndeterminate = false
                        holder.noMore.visibility = View.VISIBLE
                    }

                    else -> {}
                }

            }

            is MessageViewHolder -> {
                val message = dataList[position]
                if (type == "atMe" || type == "atCommentMe") {
                    holder.id = message.id
                    holder.uid = message.uid
                    holder.entityType = message.entityType
                    holder.uname.text = message.username
                    ImageShowUtil.showAvatar(holder.avatar, message.userAvatar)
                    if (message.deviceTitle != "") {
                        holder.device.text = message.deviceTitle
                        val drawable: Drawable = mContext.getDrawable(R.drawable.ic_device)!!
                        drawable.setBounds(
                            0,
                            0,
                            holder.device.textSize.toInt(),
                            holder.device.textSize.toInt()
                        )
                        holder.device.setCompoundDrawables(drawable, null, null, null)
                        holder.device.visibility = View.VISIBLE
                    } else {
                        holder.device.visibility = View.GONE
                    }
                    holder.pubDate.text = DateUtils.fromToday(message.dateline)
                    val drawable1: Drawable = mContext.getDrawable(R.drawable.ic_date)!!
                    drawable1.setBounds(
                        0,
                        0,
                        holder.pubDate.textSize.toInt(),
                        holder.pubDate.textSize.toInt()
                    )
                    holder.pubDate.setCompoundDrawables(drawable1, null, null, null)

                    val drawableLike: Drawable = mContext.getDrawable(R.drawable.ic_like)!!
                    drawableLike.setBounds(
                        0,
                        0,
                        holder.like.textSize.toInt(),
                        holder.like.textSize.toInt()
                    )
                    if (message.userAction?.like == 1) {
                        DrawableCompat.setTint(
                            drawableLike,
                            ThemeUtils.getThemeAttrColor(
                                mContext,
                                rikka.preference.simplemenu.R.attr.colorPrimary
                            )
                        )
                        holder.like.setTextColor(
                            ThemeUtils.getThemeAttrColor(
                                mContext,
                                rikka.preference.simplemenu.R.attr.colorPrimary
                            )
                        )
                    } else {
                        DrawableCompat.setTint(drawableLike, mContext.getColor(android.R.color.darker_gray))
                        holder.like.setTextColor(mContext.getColor(android.R.color.darker_gray))
                    }
                    holder.like.text = message.likenum
                    holder.like.setCompoundDrawables(drawableLike, null, null, null)

                    holder.reply.text = message.replynum
                    val drawableReply: Drawable = mContext.getDrawable(R.drawable.ic_message)!!
                    drawableReply.setBounds(
                        0,
                        0,
                        holder.like.textSize.toInt(),
                        holder.like.textSize.toInt()
                    )
                    holder.reply.setCompoundDrawables(drawableReply, null, null, null)

                    holder.message.movementMethod = LinkMovementMethod.getInstance()
                    holder.message.text = SpannableStringBuilderUtil.setText(
                        mContext,
                        message.message,
                        (holder.message.textSize * 1.3).toInt(),
                        null
                    )
                } else if (type == "feedLike") {
                    holder.entityType = message.entityType
                    holder.uname.text = message.likeUsername
                    holder.uid = message.likeUid
                    ImageShowUtil.showAvatar(holder.avatar, message.likeAvatar)
                    holder.pubDate.text = DateUtils.fromToday(message.likeTime)
                    val drawable1: Drawable = mContext.getDrawable(R.drawable.ic_date)!!
                    drawable1.setBounds(
                        0,
                        0,
                        holder.pubDate.textSize.toInt(),
                        holder.pubDate.textSize.toInt()
                    )
                    holder.pubDate.setCompoundDrawables(drawable1, null, null, null)
                    holder.message.text = "赞了你的回复"
                }


                if (message.forwardSourceFeed != null) {
                    holder.forward.visibility = View.VISIBLE
                    holder.forwardEntityType = message.forwardSourceFeed.entityType
                    holder.forwardId = message.forwardSourceFeed.id
                    holder.forwardUid = message.forwardSourceFeed.uid
                    holder.forwardUname = message.forwardSourceFeed.username
                    val title =
                        """<a class="feed-link-uname" href="/u/${message.forwardSourceFeed.username}">@${message.forwardSourceFeed.username}: </a>${message.forwardSourceFeed.messageTitle}"""
                    holder.forwardTitle.movementMethod = LinkMovementMethod.getInstance()
                    holder.forwardTitle.text = SpannableStringBuilderUtil.setText(
                        mContext,
                        title,
                        (holder.forwardMessage.textSize * 1.3).toInt(),
                        null
                    )
                    holder.forwardMessage.movementMethod = LinkMovementMethod.getInstance()
                    holder.forwardMessage.text = SpannableStringBuilderUtil.setText(
                        mContext,
                        message.forwardSourceFeed.message,
                        (holder.forwardMessage.textSize * 1.3).toInt(),
                        null
                    )
                    if (!message.forwardSourceFeed.picArr.isNullOrEmpty()) {
                        holder.multiImage.visibility = View.VISIBLE
                        if (message.forwardSourceFeed.picArr.size == 1) {
                            val from = message.forwardSourceFeed.pic.lastIndexOf("@")
                            val middle = message.forwardSourceFeed.pic.lastIndexOf("x")
                            val end = message.forwardSourceFeed.pic.lastIndexOf(".")
                            val width =
                                message.forwardSourceFeed.pic.substring(from + 1, middle).toInt()
                            val height =
                                message.forwardSourceFeed.pic.substring(middle + 1, end).toInt()
                            holder.multiImage.imgHeight = height
                            holder.multiImage.imgWidth = width
                        }
                        holder.multiImage.apply {
                            val urlList: MutableList<String> = ArrayList()
                            if (PrefManager.isFullImageQuality) {
                                setUrlList(message.forwardSourceFeed.picArr)
                            } else {
                                for (element in message.forwardSourceFeed.picArr)
                                    if (element.substring(
                                            element.length - 3,
                                            element.length
                                        ) != "gif"
                                    )
                                        urlList.add("$element.s.jpg")
                                    else urlList.add(element)
                                setUrlList(urlList)
                            }
                        }
                    } else {
                        holder.multiImage.visibility = View.GONE
                    }
                } else {
                    holder.forward.visibility = View.GONE
                }

                if (message.feed != null) {
                    holder.forward1.visibility = View.VISIBLE
                    if (type == "atCommentMe") {
                        holder.forwardId1 = message.feed.id
                        holder.forwardUid1 = message.feed.uid
                        holder.forwardUname1.text = "@${message.feed.username}"
                        holder.forwardMessage1.text = SpannableStringBuilderUtil.setText(
                            mContext,
                            message.feed.message,
                            (holder.forwardMessage.textSize * 1.3).toInt(),
                            null
                        )
                        ImageShowUtil.showIMG(holder.forward1Pic, message.feed.pic)
                    } else if (type == "feedLike") {
                        holder.forwardId1 = message.fid
                        holder.forward1Pic.visibility = View.GONE
                        holder.forwardUname1.text = "@${message.username}"
                        holder.forwardMessage1.text = SpannableStringBuilderUtil.setText(
                            mContext,
                            message.message,
                            (holder.forwardMessage.textSize * 1.3).toInt(),
                            null
                        )
                    }
                } else {
                    holder.forward1.visibility = View.GONE
                }
            }
        }
    }

}