package me.rerere.awara.ui.page.video.pager

import android.Manifest
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.data.entity.Video
import me.rerere.awara.data.entity.fixUrl
import me.rerere.awara.data.entity.thumbnailUrl
import me.rerere.awara.ui.LocalMessageProvider
import me.rerere.awara.ui.component.common.Button
import me.rerere.awara.ui.component.common.ButtonType
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.ext.DynamicStaggeredGridCells
import me.rerere.awara.ui.component.ext.plus
import me.rerere.awara.ui.component.iwara.AuthorCard
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.RichText
import me.rerere.awara.ui.component.iwara.TagRow
import me.rerere.awara.ui.page.video.VideoVM
import me.rerere.awara.util.openUrl
import me.rerere.awara.util.shareLink
import me.rerere.awara.util.toLocalDateTimeString
import me.rerere.awara.worker.DownloadWorker

@Composable
fun VideoOverviewPage(vm: VideoVM) {
    val state = vm.state
    if (state.private) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            AuthorCard(user = state.privateUser, onClickSub = null)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.errors_private_video),
                )
            }
        }

    } else {
        Spin(
            show = state.loading,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize(),
                columns = DynamicStaggeredGridCells(),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(8.dp) + WindowInsets.navigationBars.asPaddingValues()
            ) {
                state.video?.let {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        VideoInfoCard(
                            video = it,
                            vm = vm
                        )
                    }
                    item(
                        span = StaggeredGridItemSpan.FullLine
                    ) {
                        AuthorCard(
                            user = it.user,
                            onClickSub = {
                                vm.followOrUnfollow()
                            }
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        TagRow(
                            tags = state.video.tags,
                        )
                    }

                    items(state.relatedVideos) {
                        MediaCard(media = it)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun VideoInfoCard(video: Video, vm: VideoVM) {
    val (expand, setExpand) = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val message = LocalMessageProvider.current
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = if (expand) Int.MAX_VALUE else 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expand) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "More",
                        modifier = Modifier
                            .clip(CircleShape)
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clickable {
                                setExpand(!expand)
                            }
                    )
                }

                if (!video.body.isNullOrBlank() && expand) {
                    RichText(
                        text = video.body.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        onLinkClick = {
                            context.openUrl(it)
                        }
                    )
                }
            }

            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = video.createdAt.toLocalDateTimeString()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.num_likes, video.numLikes)
                    )

                    Text(
                        text = stringResource(R.string.num_views, video.numViews),
                    )
                }
            }

            Row {
                val permissionState = rememberPermissionState(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    ""
                })
                IconButton(
                    onClick = {
                        val workManager = WorkManager.getInstance(context)
                        scope.launch {
                            // check notification permission
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if(permissionState.status != PermissionStatus.Granted) {
                                    message.error {
                                        Text(stringResource(R.string.please_grant_notification_permission))
                                    }
                                    permissionState.launchPermissionRequest()
                                    return@launch
                                }
                            }

                            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                                message.error { Text(stringResource(R.string.notification_is_not_enabled)) }
                                return@launch
                            }

                            if (workManager.getWorkInfosByTag(vm.id).await().any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }) {
                                message.error { Text(stringResource(R.string.already_downloading)) }
                                return@launch
                            }

                            if(vm.isVideoAlreadyDownloaded()) {
                                message.error { Text(stringResource(R.string.already_downloaded)) }
                                return@launch
                            }

                            val req = OneTimeWorkRequestBuilder<DownloadWorker>()
                                .setInputData(
                                    Data.Builder()
                                        .putString(DownloadWorker.KEY_DOWNLOAD_TITLE, video.title)
                                        .putString(
                                            DownloadWorker.KEY_DOWNLOAD_URL,
                                            vm.state.urls.last().src.download.fixUrl()
                                        )
                                        .putString(DownloadWorker.KEY_DOWNLOAD_RESOURCE_ID, vm.id)
                                        .putString(DownloadWorker.KEY_DOWNLOAD_TYPE, "VIDEO")
                                        .putString(
                                            DownloadWorker.KEY_DOWNLOAD_THUMBNAIL,
                                            video.thumbnailUrl()
                                        )
                                        .build()
                                )
                                .addTag(vm.id)
                                .build()

                            message.info {
                                Text(stringResource(R.string.start_downloading))
                            }

                            workManager.enqueue(req)
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Download, null)
                }

                IconButton(
                    onClick = { context.shareLink("https://www.iwara.tv/video/${vm.id}") }
                ) {
                    Icon(Icons.Outlined.Share, null)
                }

                IconButton(onClick = { /*TODO*/ }) {
                    Icon(Icons.Outlined.Translate, null)
                }

                Spacer(modifier = Modifier.weight(1f))

                var showPlaylistSheet by remember {
                    mutableStateOf(false)
                }
                IconButton(
                    onClick = {
                        showPlaylistSheet = true
                    }
                ) {
                    Icon(Icons.Outlined.PlaylistAdd, null)
                }
                if (showPlaylistSheet) {
                    PlaylistSheet(
                        vm = vm,
                        onDismissRequest = { showPlaylistSheet = false }
                    )
                }


                Button(
                    onClick = {
                        vm.likeOrUnlike()
                    },
                    type = if (video.liked) ButtonType.Outlined else ButtonType.Default,
                    loading = vm.state.likeLoading
                ) {
                    Text(if (video.liked) stringResource(R.string.dislike) else stringResource(R.string.like))
                }
            }
        }
    }
}