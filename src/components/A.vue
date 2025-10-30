<template>
  <div class="a-container">
<div class="paragraphs">
<h2>天然卫星数据中心</h2>
<hr>
<p>    本研究依托 30 余年巨行星卫星观测数据开展研究,涵盖 YAO100、YAO240、SHAO156等望远镜的 5 万余个幅FITS/FIT 文件(含天文图像与位置信息),为相关天然卫星的动态演化研究提供关键数据支撑。</p>
</div>
<div class="buttons">
<button class="btn" @click="$router.push('/search')">图像信息检索</button>
<button class="btn" @click="$router.push('/files')">文件数据</button>
<button class="btn" @click="$router.push('/fits-parser')">解析fits头文件</button>
</div>
<!-- 右下角的机器人按钮 -->
<button class="robot-btn" @click="$router.push('/aiagent')">
<img src="../resources/images/robot.png" alt="AI Agent">
</button>
</div>
</template>

<script>
export default {
name: 'A'
}
</script>

<style scoped>
.a-container {
flex: 1;
padding: 60px 40px;
margin: 10px 10px 20px 10px;
border: 1px solid #ebeef5;
box-shadow: 0 2px 0px 0 rgba(0, 0, 0, 0.1);
border-radius: 8px;
background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
position: relative;
min-height: 70vh;
display: flex;
flex-direction: column;
justify-content: center;
}

.paragraphs {
margin-bottom: 50px;
line-height: 2;
max-width: 800px;
margin-left: auto;
margin-right: auto;
}

.paragraphs h2 {
font-size: 32px;
color: #2c3e50;
margin-bottom: 20px;
text-align: center;
}

.paragraphs hr {
border: none;
height: 2px;
background: linear-gradient(to right, transparent, #409eff, transparent);
margin: 20px 0 30px 0;
}

.paragraphs p {
font-size: 16px;
color: #606266;
text-indent: 2em;
}

.buttons {
display: flex;
flex-direction: column;
gap: 15px;
max-width: 400px;
margin: 0 auto;
}

.btn {
background: linear-gradient(135deg, #706c6c 0%, #68656a 100%);
color: #fff;
border: none;
padding: 15px 30px;
border-radius: 8px;
cursor: pointer;
text-align: center;
font-size: 16px;
font-weight: 500;
transition: all 0.3s ease;
box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.btn:hover {
transform: translateY(-2px);
box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 右下角机器人按钮样式 */
.robot-btn {
position: absolute;
bottom: 20px;
right: 20px;
width: 60px;
height: 60px;
border-radius: 50%;
border: none;
background-color: #fff;
box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
cursor: pointer;
padding: 8px;
transition: all 0.3s ease;
z-index: 100;
}

.robot-btn:hover {
transform: scale(1.1);
box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.robot-btn img {
width: 100%;
height: 100%;
object-fit: contain;
}
</style>