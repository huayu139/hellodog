#!/usr/bin/env bash
cd "$(dirname "$0")"
export COPILOT_CONFIG_DIR=$(pwd)/.copilot
copilot -p "在主界面中，先显示两个按钮，一个按钮是搜索蓝牙设备，一个按钮是连接已经保存的蓝牙设备。"
