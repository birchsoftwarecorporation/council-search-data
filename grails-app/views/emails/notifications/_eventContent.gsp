<!-- Event -->
<div class="title" style="color: #303030; font-size: 16px; font-weight: bold;"><%= eventMap.get("title") %></div>
<div class="meta text-muted " style="color: #636c72; font-size: 12px;">
    <%= eventMap.get("meetingDate") %> - <%= eventMap.get("geography") %> - <%= eventMap.get("documentType") %>
</div>
<table class="s-2 w-100" border="0" cellpadding="0" cellspacing="0" style="width: 100%;">
    <tbody>
    <tr>
        <td height="8" style="border-spacing: 0px; border-collapse: collapse; line-height: 8px; font-size: 8px; width: 100%; height: 8px; margin: 0;" align="left">

        </td>
    </tr>
    </tbody>
</table>


<table class="pb-2" border="0" cellpadding="0" cellspacing="0" style="font-family: Helvetica, Arial, sans-serif; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0px; border-collapse: collapse;">
    <tbody>
    <tr>
        <td style="border-spacing: 0px; border-collapse: collapse; line-height: 24px; font-size: 16px; padding-bottom: 8px; margin: 0;" align="left">
            <p class="" style="line-height: 24px; font-size: 16px; width: 100%; margin: 0;" align="left">
                <%= eventMap.get("preview") %>
            </p>
        </td>
    </tr>
    </tbody>
</table>

<table class="btn btn-primary" border="0" cellpadding="0" cellspacing="0" style="font-family: Helvetica, Arial, sans-serif; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0px; border-collapse: separate !important; border-radius: 4px;">
    <tbody>
    <tr>
        <td style="border-spacing: 0px; border-collapse: collapse; line-height: 24px; font-size: 16px; border-radius: 4px; margin: 0;" align="center" bgcolor="#007bff">
            <a href="<%= baseUrl+'/event/show/'+eventMap.get("uuid") %>" style="font-size: 16px; font-family: Helvetica, Arial, sans-serif; text-decoration: none; border-radius: 4px; line-height: 20px; display: inline-block; font-weight: normal; white-space: nowrap; background-color: #007bff; color: #ffffff; padding: 8px 12px; border: 1px solid #007bff;">
                View Event
            </a>
        </td>
    </tr>
    </tbody>
</table>

<div class="hr " style="width: 100%; margin: 20px 0; border: 0;">
    <table border="0" cellpadding="0" cellspacing="0" style="font-family: Helvetica, Arial, sans-serif; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0px; border-collapse: collapse; width: 100%;">
        <tbody>
        <tr>
            <td style="border-spacing: 0px; border-collapse: collapse; line-height: 24px; font-size: 16px; border-top-width: 1px; border-top-color: #dddddd; border-top-style: solid; height: 1px; width: 100%; margin: 0;" align="left"></td>
        </tr>
        </tbody>
    </table>
</div>

<!-- End Event -->