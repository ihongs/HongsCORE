<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<ol class="backable breadcrumb row hide" data-toggle="hsTabs">
    <li class="back-crumb dont-close pull-right">
        <a href="javascript:;">
            <i class="glyphicon glyphicon-remove-sign"></i>
        </a>
    </li>
    <li class="home-crumb active">
        <a href="javascript:;">
            <i class="glyphicon glyphicon-folder-open"></i>
            <b></b>
        </a>
    </li>
</ol>
<div class="backable panes">
    <div class="row"></div>
    <div class="row"><%@include file="list.jsp"%></div>
</div>