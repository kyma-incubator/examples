const Sentiment = require('sentiment');
const sentiment = new Sentiment();
const axios = require("axios");


function isPositive(txt) {
    let result = sentiment.analyze(txt);
    console.log("Comment: %s\nSentiment: %s", txt, result);
    return result.comparative>0.2;
}

async function setCommentStatus(id, status) {
    let commentUrl = `${process.env.WP_GATEWAY_URL}/wp/v2/comments/${id}?status=${status}`;
    const update = await axios.post(commentUrl);
    return update;
}

async function getComment(id) {
    let commentUrl = `${process.env.WP_GATEWAY_URL}/wp/v2/comments/${id}?context=edit`
    let response = await axios.get(commentUrl);
    return response.data;
}

module.exports = {
    main: async function (event, context) {
        console.log("Gateway URL: %s", process.env.WP_GATEWAY_URL);
        let status = "hold";
        let comment = await getComment(event.data.commentId);
        let positive = await isPositive(comment.content.raw);
        if (positive) {
            status = "approved"
        } 
        setCommentStatus(comment.id, status);
    }
};