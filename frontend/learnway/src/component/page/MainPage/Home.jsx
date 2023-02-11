import React from "react";
import styled from "styled-components";
import NavBar from "../../ui/NavBar";
import Body from "./Body";
import Animation from "./MainAnimation";
import cloudImg from './cloud.png';

const BackGround = styled.div`
    width:100%;
    background-image: url(${cloudImg});
    // background-repeat: no-repeat;
`;


function Home(params) {
    return (
        <div>
            <NavBar></NavBar>
            <Animation></Animation>
            {/* <Body></Body> */}
            <BackGround>
                <Body></Body>
            </BackGround>
            
        </div>
    );
}
export default Home;
