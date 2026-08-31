<template>

	<!-- 上传器 -->
	<uploader ref="uploaderRef" :options="options" :autoStart="true" class="uploader-ui" @file-added="onFileAdded"
		@complete="onUploadComplete" @files-added="onFilesAdded" @file-success="onFileSuccess"
		@files-submitted="onFilesSubmitted" @file-error="onFileError">
		<uploader-btn :attrs="attrs" id="uploadImage" style="visibility: hidden;"></uploader-btn>
		<uploader-btn :directory="true" id="uploadFolder" style="visibility: hidden;"></uploader-btn>
		<uploader-files class="upload-list" v-show="ShowPanel">
			<template #default="props">
				<div class="upload-list-header">
					<span class="upload-list-header-item" style="margin-right:auto">{{ uploadStatus }}</span>
					<el-button type="primary" link class="upload-list-header-item"
						@click="() => { AllPaused(isAllPaused) }">{{ isAllPaused ? '全部开始' : '全部暂停' }}
					</el-button>
					<el-button color="#979797" link class="upload-list-header-item"
						@click="ShowPanelInfo = !ShowPanelInfo">
						<i-ep:ArrowDownBold v-if="ShowPanelInfo" />
						<i-ep:ArrowUpBold v-else />
					</el-button>
					<el-button color="#979797" link class="upload-list-header-item" @click="uploadClose">
						<i-ep:CloseBold />
					</el-button>
				</div>
				<div class="upload-list-content" v-show="ShowPanelInfo">
					<el-row :gutter="4">
						<el-col :span="3" class="upload-list-header-info-item">
							类型
						</el-col>
						<el-col :span="8" class="upload-list-header-info-item">
							文件名称
						</el-col>
						<el-col :span="4" class="upload-list-header-info-item">
							大小
						</el-col>
						<el-col :span="6" class="upload-list-header-info-item">
							状态
						</el-col>
						<el-col :span="1.5" class="upload-list-header-info-item">
						</el-col>
						<el-col :span="1.5" class="upload-list-header-info-item">

						</el-col>
					</el-row>
					<div class="upload-list-header-info">

						<div v-for="(file, index) in (props.files)" :key="file.id" :index="index">

							<uploader-file ref="uploadfiles" :file="file" :list="false" class="upload-list-header-file">
								<template #default="fileProps">
									<div class="upload-list-progress"
										:style="{ width: (fileProps.progress * 100) + '%' }">
									</div>
									<el-row :gutter="4">
										<el-col :span="2.5" class="upload-list-header-info-item">

											<i-catppuccin:image v-if="acceptImageType.includes('.' + fileProps.type)"
												style="font-size:45px;" />
											<i-catppuccin:video v-else style="font-size: 45px;" />
										</el-col>
										<el-col :span="8" class="upload-list-header-info-item">
											{{ file.name }}
										</el-col>
										<el-col :span="4" class="upload-list-header-info-item">
											{{ fileProps.formatedSize }}
										</el-col>
										<el-col :span="6" class="upload-list-header-info-item">
											<div v-show="fileProps.status === 'uploading'">{{
												(fileProps.progress * 100).toFixed(2)
												+ '%(' + fileProps.formatedAverageSpeed + ')' }}</div>
											<div v-show="fileProps.status === 'success'"
												style="display:flex ;align-items: center;">
												<i-qlementine-icons:success-16
													style="color: #3b75ff;margin-right: 2px;" />上传完成
											</div>
											<div v-show="fileProps.status === 'paused'"><span
													style="margin-right: 15px;"></span>暂停上传</div>
											<div v-show="fileProps.status === 'waiting'"><span
													style="margin-right: 15px;"></span>等待上传</div>
											<div v-show="fileProps.status === 'error'"
												style="display:flex ;align-items: center;">
												<i-qlementine-icons:clear-16
													style="color: red;margin-right: 2px;" />上传失败
											</div>
										</el-col>
										<el-col :span="1.5" class="upload-list-header-info-item">
											<el-button v-show="fileProps.status == 'error'" color="#979797" link
												@click="() => { uploadfiles[index].retry() }">
												<i-qlementine-icons:refresh-16 style="font-size: 18px;" />
											</el-button>
											<div v-show="fileProps.status != 'success' && fileProps.status != 'error'">


												<el-button v-if="fileProps.status === 'uploading'" color="#979797" link
													@click="() => { uploadfiles[index].pause(); }">
													<i-material-symbols:pause style="font-size: 18px;" />
												</el-button>
												<el-button v-else color="#979797" link
													@click="() => { uploadfiles[index].resume(); }">
													<i-material-symbols:play-arrow style="font-size: 18px;" />
												</el-button>
											</div>
										</el-col>
										<el-col :span="1.5" class="upload-list-header-info-item">
											<el-button
												v-show="fileProps.status != 'success' && fileProps.status != 'error'"
												color="#979797" link @click="() => { file.cancel(); }">
												<i-material-symbols:close style="font-size: 18px;" />
											</el-button>
										</el-col>
									</el-row>
								</template>
							</uploader-file>
						</div>
					</div>
				</div>
			</template>
		</uploader-files>

	</uploader>

</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import SparkMD5 from 'spark-md5';
import $bus from '@/utils/bus.ts'
import requestPublicConfig from "@/api/config";
const options = reactive({
	//目标上传 URL，默认POST, import.meta.env.VITE_API_URL = api
	target: requestPublicConfig.baseUrl + '/file/upload',
	query: (file) => {
		// 上传参数
		return {
			lastModified: file.file.lastModified,
			contentType: file.file.type,
			albumId: albumId.value,
		};
	},

	headers: {
		// 需要携带token信息
		Authorization: localStorage.getItem('token'),
	},
	//分块大小(单位：字节) 默认1000M
	chunkSize: 1024 * 1024 * 1000,
	// 并发上传数
	simultaneousUploads: 4,
	//上传文件时文件内容的参数名，对应chunk里的Multipart对象名，默认对象名为file
	fileParameterName: 'multipartFile',
	//失败后最多自动重试上传次数
	maxChunkRetries: 1,
	//是否开启服务器分片校验，对应GET类型同名的target URL
	testChunks: true,
	// 服务器分片校验函数
	checkChunkUploadedByResponse: function (chunk, response) {
		let resp = JSON.parse(response);
		if (resp.code != '200') {
			return true;
		}
		if (resp.message == "exist") {
			return true;
		}
		return false;
		// return (objMessage.uploadedChunks || []).indexOf(chunk.offset + 1) >= 0;

	},
});

const acceptImageType = ['.png', '.jpg', '.jpeg', '.gif', '.bmp'];
const acceptVedioType = ['.mp4', '.rmvb', '.mkv', '.wmv', '.flv'];
// 上传文件类型
const acceptType = ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.mp4', '.rmvb', '.mkv', '.wmv', '.flv'];
// 可接受的上传文件类型
const attrs = reactive({
	accept: acceptType,
});
// 是否显示上传列表
const ShowPanel = ref(false);
const ShowPanelInfo = ref(true);
// 上传REF 
const uploaderRef = ref();
// 是否全部暂停
const isAllPaused = ref(true);
// 所有文件加入列表 后的ref
const uploadfiles = ref([]);


const uploadStatus = computed(() => {
	let successCount = 0;
	let pasuedCount = 0;
	const len = uploadfiles.value.length;
	uploadfiles.value.forEach((file) => {
		if (file.status === 'success') {
			successCount++;
		}
		if (file.status === 'paused') {
			pasuedCount++;
		}
	})

	return pasuedCount == len ? '上传暂停中 (' + pasuedCount + '/' + len + ')' : successCount != len ? '正在上传中 (' + successCount + '/' + len + ')' : '全部上传完成';

});
onMounted(() => {
	// 组件通信，展示上传列表
	showUplaodList();
});
//相册id
const albumId = ref(-1);
// 组件通信，展示上传列表
const showUplaodList = () => {
	// 先清除之前的监听事件
	$bus.off('showUploadList');
	$bus.on('showUploadList', (uploadParams) => {
		options.query = {
			albumId: uploadParams.albumId,
		}
		document.getElementById(uploadParams.uploadType).click();
		albumId.value = uploadParams.albumId;
	});
}

function onFilesSubmitted(files, fileList) {

}
// 所有文件校验完，显示上传列表
function onFilesAdded(files) {
	ShowPanel.value = true;
	// // 暂停文件
	// files.forEach(file => {
	// 	file.pause();
	// });
}
// 符合接受类型的图片才能上传
function onFileAdded(file) {
	// 校验文件
	const type = '.' + file.getType()
	computeMD5(file);
	return acceptType.includes(type);
}
function onFileSuccess(rootFile, file, response, chunk) {

}
function onUploadComplete() {
	ShowPanel.value = false;
	uploaderRef.value.uploader.cancel();

	ElNotification({
		title: '上传完成',
		type: 'success',
		message: uploadfiles.value.length + '个照片上传完成',
		position: 'bottom-right',
		offset: 100,
	});
	uploaderRef.value.uploader.cancel();
	//上传完成，通知其他组件刷新数据,传入相册id代表哪种方式上传
	$bus.emit('uploadSucess',albumId.value);

}
function onFileError(rootFile, file, response, chunk) {
	ElMessage.error('上传失败：' + response);
}

/**
 * 计算md5，实现断点续传及秒传
 * @param file
 */
function computeMD5(file) {
	// 先暂停上传，等MD5计算完毕再继续
	file.pause();
	//单个文件的大小限制2G
	let fileSizeLimit = 2 * 1024 * 1024 * 1024;
	if (file.size > fileSizeLimit) {
		file.cancel();
	}
	let fileReader = new FileReader();
	let time = new Date().getTime();
	let blobSlice =
		File.prototype.slice ||
		File.prototype.mozSlice ||
		File.prototype.webkitSlice;
	let currentChunk = 0;
	// 1000M
	const chunkSize = 1024 * 1024 * 1000;
	let chunks = Math.ceil(file.size / chunkSize);
	let spark = new SparkMD5.ArrayBuffer();
	//由于计算整个文件的Md5太慢，因此采用只计算第1块文件的md5的方式
	let chunkNumberMD5 = 1;
	loadNext();
	fileReader.onload = (e) => {
		spark.append(e.target.result);

		if (currentChunk < chunkNumberMD5) {
			loadNext();
		} else {
			let md5 = spark.end();
			file.uniqueIdentifier = md5;
			file.resume();
		}
	};
	fileReader.onerror = function () {
		error(`文件${file.name}读取出错，请检查该文件`);
		file.cancel();
	};
	function loadNext() {
		let start = currentChunk * chunkSize;
		let end = start + chunkSize >= file.size ? file.size : start + chunkSize;

		fileReader.readAsArrayBuffer(blobSlice.call(file.file, start, end));
		currentChunk++;
	}
}

const AllPaused = (b) => {
	isAllPaused.value = !b;
	if (!b) {
		uploadfiles.value.forEach(file => {
			file.pause();
		});
	}
	else {
		uploadfiles.value.forEach(file => {
			file.resume();
		});
	}
}

// 取消上传
const uploadClose = () => {
	ElMessageBox.confirm(
		'列表里还有未上传完成的文件，确认取消上传?',
		'取消上传',
		{
			confirmButtonText: '放弃上传',
			cancelButtonText: '取消',
			type: 'warning',
		}
	).then(() => {
		ElMessage.success('上传取消');
		uploaderRef.value.uploader.cancel();
		ShowPanel.value = false;
	})
		.catch(() => {
		})

}

function error(msg) {
	ElMessage.error(msg);
}
</script>

<style scoped>
.upload-list {
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

		max-height: 350px;
		width: 338px;
	}
}

.upload-list-header {
	display: flex;
	justify-content: flex-end;
	height: 35px;
	align-items: center;
}

.upload-list-header-item {
	font-size: 16px;
	font-weight: 600;
}

.upload-list-content {
	margin-top: 20px;
}

.upload-list-header-info {
	margin-top: 5px;
	max-height: 328px;
	overflow-y: auto;
}

.upload-list-header-file {
	border-bottom: 3px solid white;
}

/* 进度条 */
.upload-list-progress {
	background: rgb(239, 242, 261);
	width: 0%;
	height: 48px;
	position: absolute;
	border-radius: 10px;
	transition: width 0.5s ease-in-out;
}

.upload-list-header-info-item {
	font-size: 13px;
	color: #666;
	align-items: center;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
</style>