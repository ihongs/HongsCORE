/**
 * File 国际化及初始化
 * Huang Hong <ihongs@live.cn>
 * @param $ jQuery
 */
(function($) {
    if (!$.fn.fileinput) {
        return;
    }

    $.fn.fileinputLocales.en = {
        fileSingle      : hsGetLang('file.single'),
        filePlural      : hsGetLang('file.plural'),
        browseLabel     : hsGetLang('file.select'),
        removeLabel     : hsGetLang('file.remove'),
        removeTitle     : hsGetLang('file.remove.title'),
        uploadLabel     : hsGetLang('file.upload'),
        uploadTitle     : hsGetLang('file.upload.title'),
        cancelLabel     : hsGetLang('file.cancel'),
        cancelTitle     : hsGetLang('file.cancel.title'),
        dropZoneTitle   : hsGetLang('file.drop.to.here'),
        msgSizeTooLarge         : hsGetLang('file.invalid.size'),
        msgInvalidFileType      : hsGetLang('file.invalid.type'),
        msgInvalidFileExtension : hsGetLang('file.invalid.extn'),

        fileActionSettings: {
            removeTitle : hsGetLang('file.remove'),
            uploadTitle : hsGetLang('file.upload'),

            indicatorNewTitle     : 'Not uploaded',
            indicatorLoadingTitle : 'Uploading...',
            indicatorSuccessTitle : 'Uploaded !!!',
            indicatorErrorTitle   : 'Upload error'
        },

        msgLoading              : 'Loading file {index} of {files} &hellip;',
        msgProgress             : 'Loading file {index} of {files} - {name} - {percent}% completed.',
        msgSelected             : '{n} {files} selected',
        msgZoomTitle            : 'View details',
        msgZoomModalHeading     : 'Detailed Preview',
        msgFilesTooLess         : 'You must select at least <b>{n}</b> {files} to upload.',
        msgFilesTooMany         : 'Number of files selected for upload <b>({n})</b> exceeds maximum allowed limit of <b>{m}</b>.',
        msgFileSecured          : 'Security restrictions prevent reading the file "{name}".',
        msgFileNotFound         : 'File "{name}" not found!',
        msgFileNotReadable      : 'File "{name}" is not readable.',
        msgFilePreviewAborted   : 'File preview aborted for "{name}".',
        msgFilePreviewError     : 'An error occurred while reading the file "{name}".',
        msgUploadAborted        : 'The file upload was aborted',
        msgValidationError      : 'File Upload Error',
        msgFoldersNotAllowed    : 'Drag & drop files only! Skipped {n} dropped folder(s).',

        msgImageWidthSmall      : 'Width of image file "{name}" must be at least {size} px.',
        msgImageHeightSmall     : 'Height of image file "{name}" must be at least {size} px.',
        msgImageWidthLarge      : 'Width of image file "{name}" cannot exceed {size} px.',
        msgImageHeightLarge     : 'Height of image file "{name}" cannot exceed {size} px.',
        msgImageResizeError     : 'Could not get the image dimensions to resize.',
        msgImageResizeException : 'Error while resizing the image.<pre>{errors}</pre>'
    };

    $.extend($.fn.fileinput.defaults, $.fn.fileinputLocales.en);

    var initialPreview = function(type, vals, name) {
        var f;
        switch (type) {
            case "image":
                f = function(u) {
                    return '<img src="'+u+'" class="file-preview-image">';
                };
                break;
            case "video":
                f = function(u) {
                    return '<video controls><source src="'+u+'"></video>';
                };
                break;
            case "audio":
                f = function(u) {
                    return '<audio controls><source src="'+u+'"></audio>';
                };
                break;
            case "flash":
                f = function(u) {
                    return '<object class="file-object" data="'+u+'" type="application/x-shockwave-flash"></object>';
                };
                break;
            default:
                f = function(u) {
                    return '<object class="file-object" data="'+u+'"></object>';
                };
                break;
        }

        for (var i = 0, j = vals.length; i < j; i ++) {
            vals[i] = f(vals[i])+'<input type="hidden" name="'+name+'" value="'+vals[i]+'"/>';
        }
        return vals;
    };

    $.fn.hsFileInput = function() {
        $(this).each ( function() {
            if ($(this).data( "fileinput" )) {
                return;
            }

            var that = $(this);
            var attr;
            var opts;

            var mrep = function(v) {
                if (!/^(\{.*\})$/.test( v )) {
                        v  = '{'+v+'}' ;
                }
                return  eval('('+v+')');
            };

            // 基础配置
            attr = that.attr("data-config" );
            if (attr) {
                opts = mrep.call(this, attr);
            } else {
                opts = {};
            }
            if (opts.showRemove  === undefined) {
                opts.showRemove   =  false;
            }
            if (opts.showCancel  === undefined) {
                opts.showCancel   =  false;
            }
            if (opts.showUpload  === undefined) {
                opts.showUpload   =  false;
            }
            if (opts.showCaption === undefined) {
                opts.showCaption  =  false;
            }
            if (opts.browseClass === undefined) {
                opts.browseClass  =  "btn btn-default form-control";
            }

            // 类型配置
            attr = that.attr("accept");
            if (attr) {
                opts.previewFileType  = attr.replace(/\/.*$/, "");
            }
            attr = that.attr("data-type" );
            if (attr) {
                opts.previewFileType  = attr;
            }
            attr = that.attr("data-types");
            if (attr) {
                opts.allowedFileTypes = attr.split(",");
            }
            attr = that.attr("data-extns");
            if (attr) {
                opts.allowedFileExtensions = attr.split(",");
            }

            // 初始配置
            attr = that.attr("data-value");
            if (attr) {
                opts.initialPreview = initialPreview(opts.previewFileType, attr.split(","), that.attr("name"));
            }

            that.removeAttr ("data-value");
            that.removeClass("input-file");
            that.fileinput(opts);

            // 重载取值
            that.on("change" , function( ) {
            attr = that.attr("data-value");
            if (attr != undefined) {
                var  optz = {/**/};
                optz.initialPreview = initialPreview(opts.previewFileType, attr.split(","), that.attr("name"));
                that.fileinput("refresh", optz);
            }
            });
        });
        return  this;
    };

    $(document).on("hsReady", function() {
        $(this).find("[data-toggle=fileinput]").hsFileInput();
    });
})(jQuery);
