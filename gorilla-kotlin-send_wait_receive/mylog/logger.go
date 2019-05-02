package mylog

import (
	"fmt"
	"log"
)

const (
	// TRACE トレース
	TRACE = iota
	// DEBUG デバッグ
	DEBUG = iota
	// INFO 情報
	INFO = iota
	// WARN 警告
	WARN = iota
	// ERROR エラー
	ERROR = iota
	// FATAL 致命的
	FATAL = iota
)

// LogLevel ログレベル
var LogLevel = INFO

// Infof 情報ログ
func Infof(format string, options ...interface{}) {
	if LogLevel < INFO {
		return
	}
	if len(options) == 0 {
		log.Output(2, "[INFO]"+format)
	} else {
		log.Output(2, fmt.Sprintf("[INFO]"+format, options))
	}
}

// Errorf エラーログ
func Errorf(format string, options ...interface{}) {
	if LogLevel < ERROR {
		return
	}
	if len(options) == 0 {
		log.Output(2, "[ERROR]"+format)
	} else {
		log.Output(2, fmt.Sprintf("[ERROR]"+format, options))
	}
}

// Error エラーログ
func Error(options ...interface{}) {
	if LogLevel < ERROR {
		return
	}
	log.Output(2, fmt.Sprintf("[ERROR]%v", options))
}
