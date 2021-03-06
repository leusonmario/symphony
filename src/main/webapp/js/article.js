/*
 * Copyright (c) 2012-2015, b3log.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @fileoverview article page and add comment.
 *
 * @author <a href="http://vanessa.b3log.org">Liyuan Li</a>
 * @version 1.1.2.2, Apr 3, 2014
 */

/**
 * @description Add comment function.
 * @static
 */
var Comment = {
    _validateData: [{
            "id": "commentContent",
            "type": 1000,
            "msg": Label.commentErrorLabel
        }],
    /**
     * @description 添加评论
     */
    add: function(id) {
        if (Validate.goValidate(this._validateData)) {
            var requestJSONObject = {
                articleId: id,
                commentContent: $("#commentContent").val().replace(/(^\s*)|(\s*$)/g, "")
            };

            $.ajax({
                url: "/comment",
                type: "POST",
                cache: false,
                data: JSON.stringify(requestJSONObject),
                beforeSend: function() {
                    $(".form button.green").attr("disabled", "disabled").css("opacity", "0.3");
                },
                success: function(result, textStatus) {
                    $(".form button.green").removeAttr("disabled").css("opacity", "1");
                    if (result.sc) {
                        window.location.reload();
                    } else {
                        $("#commentContent").next().addClass("tip-error").text(result.msg);
                    }
                },
                complete: function() {
                    $(".form button.green").removeAttr("disabled").css("opacity", "1");
                }
            });
        }
    },
    /**
     * @description 预览文章
     */
    preview: function() {
        $.ajax({
            url: "/markdown",
            type: "POST",
            cache: false,
            data: {
                markdownText: $("#commentContent").val()
            },
            success: function(result, textStatus) {
                $(".dialog-background").height($("body").height());
                $("#preview").dialog("open");
                $("#preview").html(result.html);
            }
        });
    },
    /**
     * @description 点击回复评论时，把当楼层的用户名带到评论框中
     * @param {String} userName 用户名称
     */
    replay: function(userName) {
        $("#commentContent").focus();
        var textarea = $("#commentContent").get(0),
                position = {},
                content = textarea.value;
        if (textarea.setSelectionRange) {
            // W3C
            position.end = textarea.selectionEnd;
            position.start = textarea.selectionStart;
            position.text = textarea.value.substring(position.start, position.end);
        } else if (document.selection) {
            // IE
            var i = 0,
                    oS = document.selection.createRange(),
                    // Don't: oR = textarea.createTextRange()
                    oR = document.body.createTextRange();
            oR.moveToElementText(textarea);
            position.text = oS.text;
            oS.getBookmark();
            // object.moveStart(sUnit [, iCount])
            // Return Value: Integer that returns the number of units moved.
            for (i = 0; oR.compareEndPoints('StartToStart', oS) < 0 && oS.moveStart("character", -1) !== 0; i++) {
                // Why? You can alert(textarea.value.length)
                if (textarea.value.charAt(i) == '/n') {
                    i++;
                }
            }

            position.start = i;
            position.end = i + position.text.length;
        }

        textarea.value = content.substring(0, position.start) + userName + content.substring(position.end, content.length);

        if (textarea.setSelectionRange) {
            textarea.setSelectionRange(position.start + userName.length, position.start + userName.length);
        } else {
            var oR = textarea.createTextRange();
            oR.collapse(true);
            oR.moveStart('character', position.start + userName.length);
            oR.moveEnd('character', position.start + userName.length);
            oR.select();
        }
    }
};

var Article = {
    /**
     * @description 初识化发文页面
     */
    init: function() {
        $("#commentContent").val("").keyup(function(event) {
            var $commentContent = $(this);
            if (Validate.goValidate(Comment._validateData)) {
                $commentContent.next().removeClass("tip-error").text("");
                $commentContent.parent().find(".green").show();
            } else {
                $commentContent.next().addClass("tip-error").text(Label.commentErrorLabel);
                $commentContent.parent().find(".green").hide();
            }

            if (event.keyCode === 13 && event.ctrlKey) {
                Comment.add(Label.articleOId);
            }

            this.rows = this.value.split("\n").length;
            while (this.scrollHeight
                    - (parseInt($(this).css("padding-top")) + parseInt($(this).css("padding-bottom")))
                    > $(this).height()) {
                this.rows += 1;
            }
            this.rows += 1;

            if (this.rows < 3) {
                this.rows = 3;
            }
        });

        $("#preview").dialog({
            "modal": true,
            "hideFooter": true
        });
        this.share();
        this.parseLanguage();
    },
    /**
     * @description 分享按钮
     */
    share: function() {
        $(".share span").click(function() {
            var key = $(this).data("type");
            var title = encodeURIComponent(Label.articleTitle + " - " + Label.symphonyLabel),
                    url = "http://symphony.b3log.org" + Label.articlePermalink,
                    pic = $(".content-reset img").attr("src");
            var urls = {};
            urls.tencent = "http://share.v.t.qq.com/index.php?c=share&a=index&title=" + title +
                    "&url=" + url + "&pic=" + pic;
            urls.weibo = "http://v.t.sina.com.cn/share/share.php?title=" +
                    title + "&url=" + url + "&pic=" + pic;
            urls.google = "https://plus.google.com/share?url=" + url;
            urls.twitter = "https://twitter.com/intent/tweet?status=" + title + " " + url;
            window.open(urls[key], "_blank", "top=100,left=200,width=648,height=618");
        });
    },
    /*
     * @description 解析语法高亮
     */
    parseLanguage: function() {
        var isPrettify = false;

        $(".content-reset pre, .content-reset > p > code").each(function() {
            this.className = "prettyprint";
            isPrettify = true;
        });

        if (isPrettify) {
            prettyPrint();
        }
    }
};

Article.init();