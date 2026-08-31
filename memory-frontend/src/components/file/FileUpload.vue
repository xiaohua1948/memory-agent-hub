<template>
	<div class="upload-panel-wrap">
		<!-- 隐藏选择器：id 与触发方式保持原样，各页面通过 $bus 触发 click -->
		<input id="uploadImage" ref="imageInputRef" type="file" multiple class="hidden-input"
			:accept="ACCEPT_TYPES.join(',')" @change="onImageChange" />
		<input id="uploadFolder" ref="folderInputRef" type="file" multiple class="hidden-input"
			@change="onFolderChange" />

		<div v-show="showPanel" class="upload-list">
			<div class="upload-list-header">
				<span class="upload-list-header-item status-text">{{ statusText }}</span>
				<el-button color="#979797" link @click="clearCompleted">清空已完成</el-button>
				<el-button type="primary" link @click="toggleAll">
					{{ allPaused ? '全部开始' : '全部暂停' }}
				</el-button>
				<el-button color="#979797" link @click="showDetail = !showDetail">
					<i-ep:ArrowDownBold v-if="showDetail" />
					<i-ep:ArrowUpBold v-else />
				</el-button>
				<el-button color="#979797" link @click="closePanel">
					<i-ep:CloseBold />
				</el-button>
			</div>

			<div v-show="showDetail" class="upload-list-content">
				<div v-if="uploadState.list.length === 0" class="empty-tip">暂无上传任务</div>

				<div v-for="item in uploadState.list" :key="item.id" class="upload-item">
					<div class="upload-item-progress" :style="{ width: item.progress + '%' }"></div>

					<div class="upload-item-body">
						<div class="cell cell-icon">
							<i-catppuccin:image v-if="item.category === 'image'" class="file-icon" />
							<i-catppuccin:video v-else class="file-icon" />
						</div>

						<div class="cell cell-name" :title="item.name">{{ item.name }}</div>

						<div class="cell cell-size">{{ formatSize(item.size) }}</div>

						<div class="cell cell-status">
							<span :class="['status-label', 'status-' + item.status]">{{ statusLabel(item) }}</span>
							<span v-if="item.status === 'uploading' && item.speed > 0" class="file-speed">
								{{ formatSpeed(item.speed) }}
							</span>
						</div>

						<div class="cell cell-ops">
							<el-button v-if="item.status === 'error'" color="#979797" link title="重试"
								@click="uploadManager.retry(item)">
								<i-qlementine-icons:refresh-16 class="op-icon" />
							</el-button>

							<el-button v-if="item.status === 'paused'" color="#979797" link title="继续"
								@click="uploadManager.resume(item)">
								<i-material-symbols:play-arrow class="op-icon" />
							</el-button>

							<el-button v-else-if="isRunning(item)" color="#979797" link title="暂停"
								@click="uploadManager.pause(item)">
								<i-material-symbols:pause class="op-icon" />
							</el-button>

							<el-button color="#979797" link title="移除" @click="uploadManager.remove(item)">
								<i-material-symbols:close class="op-icon" />
							</el-button>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import $bus from '@/utils/bus';
import {
	ACCEPT_TYPES,
	uploadManager,
	uploadState,
	type UploadItem,
	type UploadStatus,
} from '@/utils/upload/uploader';

const showPanel = ref(false);
const showDetail = ref(true);
const albumId = ref(-1);

const imageInputRef = ref<HTMLInputElement>();
const folderInputRef = ref<HTMLInputElement>();

onMounted(() => {
	// webkitdirectory 非标准属性，运行时设置以避免模板类型检查报错
	folderInputRef.value?.setAttribute('webkitdirectory', '');

	$bus.off('showUploadList');
	$bus.on('showUploadList', (event) => {
		const params = event as { albumId?: number; uploadType: string };
		albumId.value = params.albumId ?? -1;
		document.getElementById(params.uploadType)?.click();
	});
});

// ---------------- 选择文件 ----------------

function handleFiles(fileList: FileList | null) {
	if (!fileList || fileList.length === 0) return;

	// 新加入文件时立即取消自动关闭定时器，避免列表刚打开就被关闭
	if (autoCloseTimer) {
		clearTimeout(autoCloseTimer);
		autoCloseTimer = null;
	}

	const rejected = uploadManager.addFiles(fileList, albumId.value);
	if (rejected.length > 0) {
		const preview = rejected.slice(0, 2).join('、');
		ElMessage.warning(
			`${rejected.length} 个文件被跳过：${preview}${rejected.length > 2 ? ' 等' : ''}`,
		);
	}
	showPanel.value = true;
}

function onImageChange(event: Event) {
	const input = event.target as HTMLInputElement;
	handleFiles(input.files);
	// 重置以便重复选择同一文件
	input.value = '';
}

function onFolderChange(event: Event) {
	const input = event.target as HTMLInputElement;
	handleFiles(input.files);
	input.value = '';
}

// ---------------- 统计与展示 ----------------

const stats = computed(() => {
	const list = uploadState.list;
	return {
		total: list.length,
		success: list.filter((i) => i.status === 'success').length,
		error: list.filter((i) => i.status === 'error').length,
		paused: list.filter((i) => i.status === 'paused').length,
		active: list.filter((i) =>
			['waiting', 'hashing', 'uploading', 'merging'].includes(i.status),
		).length,
	};
});

const allPaused = computed(() => stats.value.active === 0 && stats.value.paused > 0);

const statusText = computed(() => {
	const { total, success, error, active, paused } = stats.value;
	if (total === 0) return '上传列表';
	if (error > 0) return `${success}/${total} 已完成，${error} 个失败`;
	if (active > 0) return `正在上传 (${success}/${total})`;
	if (paused > 0) return `上传暂停中 (${paused}/${total})`;
	return `全部上传完成 (${total})`;
});

function isRunning(item: UploadItem): boolean {
	return ['waiting', 'hashing', 'uploading', 'merging'].includes(item.status);
}

function statusLabel(item: UploadItem): string {
	switch (item.status) {
		case 'waiting':
			return '等待上传';
		case 'hashing':
			return `计算文件指纹 ${(item.hashProgress * 100).toFixed(0)}%`;
		case 'uploading':
			return `${item.progress.toFixed(1)}%`;
		case 'paused':
			return '已暂停';
		case 'merging':
			return '合并中…';
		case 'success':
			return item.instant ? '秒传完成' : '上传完成';
		case 'error':
			return item.errorMsg || '上传失败';
		case 'canceled':
			return '已取消';
		default:
			return '';
	}
}

function formatSize(bytes: number): string {
	if (bytes >= 1024 ** 3) return `${(bytes / 1024 ** 3).toFixed(2)} GB`;
	if (bytes >= 1024 ** 2) return `${(bytes / 1024 ** 2).toFixed(2)} MB`;
	if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
	return `${bytes} B`;
}

function formatSpeed(bytesPerSecond: number): string {
	return `${formatSize(bytesPerSecond)}/s`;
}

// 全部上传完成（含失败）后 1 秒自动关闭面板
let autoCloseTimer: ReturnType<typeof setTimeout> | null = null;
watch(
	() => uploadState.list.map((item) => item.status),
	(statuses) => {
		if (autoCloseTimer) {
			clearTimeout(autoCloseTimer);
			autoCloseTimer = null;
		}
		const total = statuses.length;
		const successCount = statuses.filter((s) => s === 'success').length;
		const errorCount = statuses.filter((s) => s === 'error').length;
		if (total > 0 && total === successCount) {
			autoCloseTimer = setTimeout(() => {
				showPanel.value = false;
				// 关闭后清空已完成项，避免下次打开列表还显示
				uploadManager.clearCompleted();
				ElMessage.success(`${successCount} 个文件上传完成`);
			}, 1000);
		} else if (total > 0 && successCount + errorCount === total) {
			// 全部结束但有失败：不自动关闭，仅提示
			ElMessage.warning(`${successCount} 个完成，${errorCount} 个失败`);
		}
	},
	{ deep: true },
);

// ---------------- 操作 ----------------

function toggleAll() {
	if (allPaused.value) {
		uploadManager.resumeAll();
	} else {
		uploadManager.pauseAll();
	}
}

function clearCompleted() {
	uploadManager.clearCompleted();
}

async function closePanel() {
	if (uploadManager.hasActive()) {
		try {
			await ElMessageBox.confirm('列表里还有未上传完成的文件，确认取消上传？', '取消上传', {
				confirmButtonText: '放弃上传',
				cancelButtonText: '继续上传',
				type: 'warning',
			});
		} catch {
			return;
		}
		uploadState.list
			.filter((item) => item.status !== 'success')
			.forEach((item) => void uploadManager.cancel(item));
	}

	// 关闭面板时清空已结束的任务（成功/失败/已取消），避免下次同文件加入被去重过滤
	if (autoCloseTimer) {
		clearTimeout(autoCloseTimer);
		autoCloseTimer = null;
	}
	const terminalStatuses: UploadStatus[] = ['success', 'error', 'canceled'];
	for (let i = uploadState.list.length - 1; i >= 0; i--) {
		if (terminalStatuses.includes(uploadState.list[i].status)) {
			uploadState.list.splice(i, 1);
		}
	}
	showPanel.value = false;
}
</script>

<style scoped>
.hidden-input {
	display: none;
}

.upload-list {
	position: fixed;
	right: 20px;
	bottom: 20px;
	z-index: 1000;
	max-height: 428px;
	width: 480px;
	box-shadow: 0 2px 14px 0 rgba(0, 0, 0, .18);
	border-radius: 10px;
	background-color: #fff;
	padding: 10px;
}

@media (max-width: 768px) {
	.upload-list {
		right: 10px;
		bottom: 10px;
		max-height: 350px;
		width: 338px;
	}
}

.upload-list-header {
	display: flex;
	justify-content: flex-end;
	align-items: center;
	gap: 2px;
	height: 35px;
}

.upload-list-header-item {
	font-size: 14px;
	font-weight: 600;
	margin-right: auto;
}

.status-text {
	color: #333;
}

.upload-list-content {
	margin-top: 6px;
}

.empty-tip {
	text-align: center;
	color: #999;
	font-size: 13px;
	padding: 16px 0;
}

.upload-list-content .upload-item {
	position: relative;
	overflow: hidden;
	border-radius: 8px;
	margin-bottom: 4px;
}

/* 进度条背景 */
.upload-item-progress {
	position: absolute;
	left: 0;
	top: 0;
	height: 100%;
	background: rgb(239, 242, 255);
	transition: width .3s ease-in-out;
}

.upload-item-body {
	position: relative;
	z-index: 1;
	display: flex;
	align-items: center;
	padding: 4px 2px;
}

.cell {
	font-size: 13px;
	color: #666;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.cell-icon {
	width: 46px;
	flex: none;
	display: flex;
	align-items: center;
	justify-content: center;
}

.file-icon {
	font-size: 34px;
}

.cell-name {
	width: 150px;
	flex: none;
	padding-right: 6px;
}

.cell-size {
	width: 74px;
	flex: none;
}

.cell-status {
	flex: 1;
	display: flex;
	align-items: center;
	gap: 6px;
	min-width: 0;
}

.status-label {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.status-success {
	color: #3b75ff;
}

.status-error {
	color: #f56c6c;
}

.status-paused,
.status-canceled {
	color: #909399;
}

.file-speed {
	color: #999;
	font-size: 12px;
	flex: none;
}

.cell-ops {
	width: 66px;
	flex: none;
	display: flex;
	align-items: center;
	justify-content: flex-end;
}

.op-icon {
	font-size: 18px;
}
</style>
