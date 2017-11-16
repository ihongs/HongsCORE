/* global self */

jQuery.fn.hsGoto = function(url, rep) {
    var $ = jQuery;
    var menu =  $(this);
    var pref = H$(".BASE_HREF");
    var cont = menu.hsFind(menu.attr("data-target"));

    if (! menu.data("hsGotoInited")) {
        menu.data("hsGotoInited", 1);
        menu.on("click", "li a" , function( ) {
            var subn = $(this);
            var text = subn.text();
            var path = subn.attr("href");
            var href = subn.attr("data-href");
            menu.find("li").removeClass("active");
            subn.closest("li").addClass("active");
            cont.hsLoad (href);
            history.pushState(null, text, hsFixUri(path));
            return false;
        });
    }

    if (! url) {
        return;
    }
    
    function cleanPath(path) {
        if (pref) {
            if (path.substr(0, pref.length) == pref) {
                path = path.substr(pref.length);
            }
        }
        return path;
    }

    function buildData(keys, mats) {
        var data;
        if (keys) {
            data = {};
            keys = keys.split(",");
            for(var i = 0, j = 1; j < mats.length; i ++, j ++) {
                data[keys[ i ]] = mats[ j ];
            }
        }
        return data;
    }

    function checkMenu(path, repl) {
        var last = true;
        var lnks = menu.find("li a");
        path = cleanPath(path);
        lnks.each(function() {
            var subn = $(this);
            var patt = new RegExp("^" + subn.data("patt") + "$");
            var mats = patt.exec( path );
            if (! mats) {
                return true;
            }
            var text = subn.text();
            var href = subn.attr("data-href");
            var keys = subn.attr("data-keys");
            var data = buildData( keys , mats );
            menu.find("li").removeClass("active");
            subn.closest("li").addClass("active");
            cont.hsLoad (href, data);
            if (repl) {
                history.replaceState(null, text, path);
            } else {
                history.   pushState(null, text, path);
            }
            last = false;
            return false;
        });
        if (last == true) {
            location.assign(hsFixUri(path));
        }
    }

    checkMenu(url, rep);

    return this;
};

//** 组件填充方法 **/

function hsDiningListFillLink(ln) {
    var id = this._info.id;
    ln.attr("href", "public/dining/food/" + id);
    ln.click(function( ) {
        self.hsGoto("public/dining/food/" + id);
        return false;
    });
    ln.closest(".itembox").data("id", id);
}

function hsDiningListBillLink(ln) {
    var id = this._info.id;
    ln.attr("href", "public/dining/bill/" + id);
    ln.click(function( ) {
        self.hsGoto("public/dining/bill/" + id);
        return false;
    });
    ln.closest(".itembox").data("id", id);
}

function hsDiningListFillLogo(ln) {
    hsDiningListFillLink.call(this, ln);
    ln.find("img")
      .attr("alt", this._info.name)
      .attr("src", this._info.picture[0]);
}

function hsDiningListBillLogo(ln) {
    hsDiningListBillLink.call(this, ln);
    ln.find("img")
      .attr("alt", this._info.name)
      .attr("src", "");
}

function hsDiningListFillDail(ln) {
    var id  = this._info.id ;
    var num = this._cart[id];
    if (num == 0) {
        ln.text( "" );
    } else {
        ln.text(num );
    }
    ln.parent().data("id", id);
}
